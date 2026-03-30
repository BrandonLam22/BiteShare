-- REVIEW ONLY.
-- Validation queries for the proposed composed app read model.
--
-- Interpretation guide:
-- - "acceptable" means the migration may continue past this check
-- - "blocking" means execution should stop until the issue is understood/fixed
-- - "manual decision" means a reviewer must explicitly approve a non-zero or mismatched result

-- 1) Raw distinct place_id count vs app row count.
-- Checks:
-- - whether the populated app table size matches the expected distinct raw place count
-- Acceptable:
-- - exact match, or a fully explained delta
-- Blocks execution:
-- - unexplained shortfall or surplus
-- Manual decision:
-- - reviewers decide whether any row-count delta is intentional
select
    (select count(*) from public.google_maps_places_app) as app_rows,
    (
        select count(*)
        from (
            select distinct place_id
            from public.google_maps_places_full
            where nullif(place_id, '') is not null
        ) s
    ) as raw_distinct_place_ids;

-- 2) Duplicate place_id should be impossible in the app table.
-- Checks:
-- - one current-state row per raw place_id
-- Acceptable:
-- - zero rows returned
-- Blocks execution:
-- - any non-zero result
-- Manual decision:
-- - none; dedupe logic must be corrected first
select place_id, count(*) as duplicate_count
from public.google_maps_places_app
group by place_id
having count(*) > 1;

-- 3) Duplicate id should also be impossible.
-- Checks:
-- - uniqueness of the app-facing stable primary key
-- Acceptable:
-- - zero rows returned
-- Blocks execution:
-- - any non-zero result
-- Manual decision:
-- - investigate legacy ID collisions before proceeding
select id, count(*) as duplicate_count
from public.google_maps_places_app
group by id
having count(*) > 1;

-- 4) Missing critical UI fields.
-- Checks:
-- - whether browse/detail/filter UI has the minimum data it needs
-- Acceptable:
-- - zero for hard-required fields, or reviewed low null rates for softer fields
-- Blocks execution:
-- - missing IDs, or materially missing name/category/city/address/rating values
-- Manual decision:
-- - reviewers decide whether missing coordinates or price values are tolerable
select
    count(*) filter (where nullif(id, '') is null) as missing_id,
    count(*) filter (where nullif(place_id, '') is null) as missing_place_id,
    count(*) filter (where nullif(name, '') is null) as missing_name,
    count(*) filter (where nullif(category_name, '') is null) as missing_category_name,
    count(*) filter (where nullif(city, '') is null) as missing_city,
    count(*) filter (where nullif(address, '') is null) as missing_address,
    count(*) filter (where rating is null) as missing_rating,
    count(*) filter (where nullif(price, '') is null) as missing_price,
    count(*) filter (where latitude is null or longitude is null) as missing_coordinates
from public.google_maps_places_app;

-- 5) Coverage of legacy-derived fields that the current app still uses.
-- Checks:
-- - whether current app behavior can be preserved after cutover
-- Acceptable:
-- - high enough coverage to avoid browse/detail/recommendation regression
-- Blocks execution:
-- - large unexplained drops in eta/distance/price_range/tags/attributes/review profiles/dietary levels
-- Manual decision:
-- - reviewers decide whether low coverage requires product or code changes before cutover
select
    count(*) as app_rows,
    count(*) filter (where nullif(eta, '') is not null) as populated_eta,
    count(*) filter (where nullif(distance, '') is not null) as populated_distance,
    count(*) filter (where nullif(price_range, '') is not null) as populated_price_range,
    count(*) filter (where jsonb_array_length(tags) > 0) as populated_tags,
    count(*) filter (where jsonb_array_length(attributes) > 0) as populated_attributes,
    count(*) filter (where review_tag_profile <> '{}'::jsonb) as populated_review_tag_profile,
    count(*) filter (where vegan_level <> 'UNKNOWN') as populated_vegan_level,
    count(*) filter (where vegetarian_level <> 'UNKNOWN') as populated_vegetarian_level,
    count(*) filter (where halal_level <> 'UNKNOWN') as populated_halal_level,
    count(*) filter (where gluten_free_level <> 'UNKNOWN') as populated_gluten_free_level,
    count(*) filter (where dairy_free_level <> 'UNKNOWN') as populated_dairy_free_level
from public.google_maps_places_app;

-- 6) Legacy mapping coverage when a legacy row exists.
-- Checks:
-- - whether legacy values were actually carried into the composed read model
-- Acceptable:
-- - low or zero "missing_*_from_legacy" counts
-- Blocks execution:
-- - systematic loss of legacy values that current app logic still uses
-- Manual decision:
-- - reviewers decide whether any missing values are expected or acceptable
select
    count(*) as rows_with_legacy_restaurant,
    count(*) filter (where nullif(app.eta, '') is null and nullif(r2.eta, '') is not null) as missing_eta_from_legacy,
    count(*) filter (where app.review_tag_profile = '{}'::jsonb and r2.review_tag_profile <> '{}'::jsonb) as missing_review_profile_from_legacy,
    count(*) filter (where jsonb_array_length(app.tags) = 0 and jsonb_array_length(coalesce(r2.tags, '[]'::jsonb)) > 0) as missing_tags_from_legacy
from public.google_maps_places_app app
join public.restaurants2 r2
    on r2.id = app.id;

select
    count(*) as rows_with_legacy_detail,
    count(*) filter (where nullif(app.distance, '') is null and nullif(rd.distance, '') is not null) as missing_distance_from_legacy,
    count(*) filter (where nullif(app.price_range, '') is null and nullif(rd.price_range, '') is not null) as missing_price_range_from_legacy,
    count(*) filter (where jsonb_array_length(app.attributes) = 0 and jsonb_array_length(coalesce(rd.attributes, '[]'::jsonb)) > 0) as missing_attributes_from_legacy
from public.google_maps_places_app app
join public.restaurant_details rd
    on rd.restaurant_id = app.id;

-- 7) Orphan checks before any future FK switch.
-- Checks:
-- - whether favorites, comments, and voting rows resolve against the new app IDs
-- Acceptable:
-- - zero orphan rows before any FK or full cutover work
-- Blocks execution:
-- - any non-zero orphan count if cutover/FK changes depend on those IDs
-- Manual decision:
-- - reviewers decide whether to backfill IDs, preserve legacy references longer, or defer cutover
select 'user_saved_restaurants' as table_name, count(*) as orphan_count
from public.user_saved_restaurants usr
left join public.google_maps_places_app app
    on app.id = usr.restaurant_id
where app.id is null
union all
select 'reviews', count(*)
from public.reviews r
left join public.google_maps_places_app app
    on app.id = r.restaurant_id
where r.restaurant_id is not null
  and app.id is null
union all
select 'vote_session_restaurants', count(*)
from public.vote_session_restaurants vsr
left join public.google_maps_places_app app
    on app.id = vsr.restaurant_id
where app.id is null
union all
select 'vote_session_votes', count(*)
from public.vote_session_votes vsv
left join public.google_maps_places_app app
    on app.id = vsv.restaurant_id
where app.id is null;

-- 8) Sample rows where the app row does not match the selected latest raw row.
-- Checks:
-- - whether the app row reflects the expected latest raw snapshot for normalized raw-derived fields
-- Acceptable:
-- - zero rows, or rows that are clearly explained by normalization or intended fallback behavior
-- Blocks execution:
-- - recurring unexplained mismatches in name/category/address/city/rating/reviews_count
-- Manual decision:
-- - reviewers inspect the sample and decide whether mismatches are acceptable
with ranked as (
    select
        g.*,
        row_number() over (
            partition by g.place_id
            order by
                g.scraped_at desc nulls last,
                g.reviews_count desc nulls last,
                g.total_score desc nulls last,
                g.title asc
        ) as rn
    from public.google_maps_places_full g
    where nullif(g.place_id, '') is not null
)
select
    a.id,
    a.place_id,
    a.name as app_name,
    r.title as raw_title,
    a.category_name as app_category,
    r.category_name as raw_category,
    a.address as app_address,
    r.address as raw_address,
    a.city as app_city,
    r.city as raw_city,
    a.rating as app_rating,
    r.total_score as raw_rating,
    a.reviews_count as app_reviews_count,
    r.reviews_count as raw_reviews_count,
    a.source_scraped_at,
    r.scraped_at as raw_scraped_at
from public.google_maps_places_app a
join ranked r
    on r.place_id = a.place_id
   and r.rn = 1
where coalesce(a.name, '') <> coalesce(r.title, '')
   or coalesce(a.category_name, '') <> coalesce(r.category_name, '')
   or coalesce(a.address, '') <> coalesce(r.address, '')
   or coalesce(a.city, '') <> coalesce(r.city, '')
   or coalesce(a.rating, -1) <> coalesce(r.total_score, -1)
   or coalesce(a.reviews_count, -1) <> coalesce(r.reviews_count, -1)
order by a.source_scraped_at desc nulls last
limit 25;

-- 9) Sample latest raw row vs app row for manual inspection.
-- Checks:
-- - a human-readable spot check of composed row quality
-- Acceptable:
-- - samples look internally consistent and match migration intent
-- Blocks execution:
-- - samples reveal obvious wrong-field mapping, stale values, or broken ID preservation
-- Manual decision:
-- - reviewers must sign off on the sample quality before any cutover
with ranked as (
    select
        g.*,
        row_number() over (
            partition by g.place_id
            order by
                g.scraped_at desc nulls last,
                g.reviews_count desc nulls last,
                g.total_score desc nulls last,
                g.title asc
        ) as rn
    from public.google_maps_places_full g
    where nullif(g.place_id, '') is not null
)
select
    a.id,
    a.place_id,
    a.name as app_name,
    r.title as raw_title,
    a.category_name as app_category,
    r.category_name as raw_category,
    a.address as app_address,
    r.address as raw_address,
    a.rating as app_rating,
    r.total_score as raw_rating,
    a.reviews_count as app_reviews_count,
    r.reviews_count as raw_reviews_count,
    a.eta,
    a.distance,
    a.tags,
    a.attributes,
    a.review_tag_profile,
    a.source_scraped_at,
    r.scraped_at as raw_scraped_at
from public.google_maps_places_app a
join ranked r
    on r.place_id = a.place_id
   and r.rn = 1
order by a.source_scraped_at desc nulls last
limit 25;

-- 10) Optional FK switch once orphan_count is zero for every table above.
-- This remains commented out on purpose.
-- It is not part of the initial migration execution package.
-- begin;
-- alter table public.user_saved_restaurants
--     drop constraint if exists user_saved_restaurants_restaurant_id_fkey,
--     add constraint user_saved_restaurants_restaurant_id_fkey
--         foreign key (restaurant_id) references public.google_maps_places_app (id)
--         on update cascade on delete cascade;
--
-- alter table public.reviews
--     drop constraint if exists reviews_restaurant_id_fkey,
--     add constraint reviews_restaurant_id_fkey
--         foreign key (restaurant_id) references public.google_maps_places_app (id)
--         on update cascade on delete set null;
--
-- alter table public.vote_session_restaurants
--     drop constraint if exists vote_session_restaurants_restaurant_id_fkey,
--     add constraint vote_session_restaurants_restaurant_id_fkey
--         foreign key (restaurant_id) references public.google_maps_places_app (id)
--         on update cascade on delete cascade;
--
-- alter table public.vote_session_votes
--     drop constraint if exists vote_session_votes_restaurant_id_fkey,
--     add constraint vote_session_votes_restaurant_id_fkey
--         foreign key (restaurant_id) references public.google_maps_places_app (id)
--         on update cascade on delete cascade;
-- commit;
