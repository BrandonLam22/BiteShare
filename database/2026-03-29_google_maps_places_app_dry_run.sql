-- REVIEW ONLY.
-- READ-ONLY diagnostics for the proposed `public.google_maps_places_app` migration.
-- This file is intended to be run directly in Supabase SQL Editor.
-- It does not create, update, or delete anything.
--
-- FINAL TYPE CONTRACT:
-- - `opening_hours` => jsonb
-- - `order_online` => jsonb
-- - `reserve_table_url` => text
--
-- JSON null-normalization rule for `opening_hours` and `order_online`:
-- Treat the following as null-equivalent:
-- - SQL NULL
-- - JSON null
-- - empty JSON array []
-- - JSON string containing only empty text
--
-- RUN INSTRUCTIONS:
-- 1) Run BLOCK 1 first.
--    Inspect:
--    - `raw_distinct_place_id_count`
--    - `expected_app_row_count_after_dedupe`
--    - `rows_reusing_existing_restaurants2_id`
--    - `rows_generating_gm_place_id`
--    - `legacy_restaurants_not_mappable_to_raw_place_id`
-- 2) Run BLOCK 2 next.
--    Inspect:
--    - critical missing counts
--    - legacy-derived coverage rates
--    - coordinate extraction rates
-- 3) Run BLOCK 3 and BLOCK 4 last.
--    Inspect:
--    - sample unmapped legacy rows
--    - sample mismatches between latest raw and proposed composed rows
--
-- NOTE:
-- Each block repeats the same CTE chain on purpose so each block can be run
-- independently in SQL Editor.

-- ============================================================
-- BLOCK 1: High-level counts and ID mapping
-- ============================================================
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
),
latest_raw as (
    select *
    from ranked
    where rn = 1
),
legacy_restaurants as (
    select *
    from public.restaurants2
),
legacy_details as (
    select *
    from public.restaurant_details
),
prepared as (
    select
        l.place_id,
        coalesce(r2.id, 'gm_' || l.place_id) as proposed_id,
        r2.id as legacy_id,
        case when r2.id is not null then true else false end as reuses_legacy_id,
        case when r2.id is null then true else false end as generates_fallback_id,
        coalesce(nullif(l.title, ''), 'Unnamed place ' || l.place_id) as proposed_name,
        nullif(l.category_name, '') as proposed_category_name,
        nullif(l.address, '') as proposed_address,
        coalesce(nullif(l.city, ''), nullif(r2.location, '')) as proposed_city,
        coalesce(nullif(l.website, ''), nullif(rd.restaurant_website, '')) as proposed_website,
        coalesce(nullif(l.url, ''), nullif(rd.google_maps_link, '')) as proposed_google_maps_url,
        nullif(l.image_url, '') as proposed_image_url,
        coalesce(nullif(l.description, ''), nullif(rd.description, ''), ''::text) as proposed_description,
        coalesce(nullif(l.price, ''), nullif(r2.price, ''), nullif(rd.price_range, '')) as proposed_price,
        coalesce(nullif(r2.price, ''), nullif(rd.price_range, ''), nullif(l.price, '')) as proposed_price_range,
        coalesce(l.total_score, r2.rating, rd.rating::double precision) as proposed_rating,
        l.reviews_count as proposed_reviews_count,
        case
            when coalesce(l.location ->> 'lat', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'lat')::double precision
            when coalesce(l.location ->> 'latitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'latitude')::double precision
            else null
        end as raw_extracted_latitude,
        case
            when coalesce(l.location ->> 'lng', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'lng')::double precision
            when coalesce(l.location ->> 'longitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'longitude')::double precision
            else null
        end as raw_extracted_longitude,
        coalesce(
            case
                when coalesce(l.location ->> 'lat', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lat')::double precision
                when coalesce(l.location ->> 'latitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'latitude')::double precision
                else null
            end,
            r2.latitude
        ) as proposed_latitude,
        coalesce(
            case
                when coalesce(l.location ->> 'lng', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lng')::double precision
                when coalesce(l.location ->> 'longitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'longitude')::double precision
                else null
            end,
            r2.longitude
        ) as proposed_longitude,
        case
            when l.opening_hours is null then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'null' then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'array'
                 and l.opening_hours::jsonb = '[]'::jsonb then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'string'
                 and btrim(l.opening_hours::jsonb #>> '{}') = '' then null
            else l.opening_hours::jsonb
        end as proposed_opening_hours,
        nullif(btrim(l.reserve_table_url), '') as proposed_reserve_table_url,
        case
            when l.order_online is null then null
            when jsonb_typeof(l.order_online::jsonb) = 'null' then null
            when jsonb_typeof(l.order_online::jsonb) = 'array'
                 and l.order_online::jsonb = '[]'::jsonb then null
            when jsonb_typeof(l.order_online::jsonb) = 'string'
                 and btrim(l.order_online::jsonb #>> '{}') = '' then null
            else l.order_online::jsonb
        end as proposed_order_online,
        coalesce(l.permanently_closed, false) as proposed_permanently_closed,
        coalesce(l.temporarily_closed, false) as proposed_temporarily_closed,
        nullif(r2.eta, '') as proposed_eta,
        nullif(rd.distance, '') as proposed_distance,
        coalesce(r2.tags, '[]'::jsonb) as proposed_tags,
        coalesce(
            rd.attributes,
            case
                when nullif(l.category_name, '') is not null then jsonb_build_array(l.category_name)
                else '[]'::jsonb
            end
        ) as proposed_attributes,
        coalesce(r2.review_tag_profile, '{}'::jsonb) as proposed_review_tag_profile,
        coalesce(nullif(r2.vegan_level, ''), 'UNKNOWN') as proposed_vegan_level,
        coalesce(nullif(r2.vegetarian_level, ''), 'UNKNOWN') as proposed_vegetarian_level,
        coalesce(nullif(r2.halal_level, ''), 'UNKNOWN') as proposed_halal_level,
        coalesce(nullif(r2.gluten_free_level, ''), 'UNKNOWN') as proposed_gluten_free_level,
        coalesce(nullif(r2.dairy_free_level, ''), 'UNKNOWN') as proposed_dairy_free_level,
        l.scraped_at as proposed_source_scraped_at,
        l.title as raw_title,
        l.category_name as raw_category_name,
        l.address as raw_address,
        l.city as raw_city,
        l.total_score as raw_total_score,
        l.reviews_count as raw_reviews_count
    from latest_raw l
    left join legacy_restaurants r2
        on r2.id = 'gm_' || l.place_id
    left join legacy_details rd
        on rd.restaurant_id = coalesce(r2.id, 'gm_' || l.place_id)
),
legacy_unmapped as (
    select r2.*
    from legacy_restaurants r2
    left join latest_raw l
        on r2.id = 'gm_' || l.place_id
    where l.place_id is null
)
select
    (select count(distinct place_id) from public.google_maps_places_full where nullif(place_id, '') is not null) as raw_distinct_place_id_count,
    (select count(*) from latest_raw) as expected_app_row_count_after_dedupe,
    (select count(*) from prepared where reuses_legacy_id) as rows_reusing_existing_restaurants2_id,
    (select count(*) from prepared where generates_fallback_id) as rows_generating_gm_place_id,
    (select count(*) from legacy_unmapped) as legacy_restaurants_not_mappable_to_raw_place_id;

-- ============================================================
-- BLOCK 2: Coverage, critical missing fields, and coordinates
-- ============================================================
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
),
latest_raw as (
    select *
    from ranked
    where rn = 1
),
legacy_restaurants as (
    select *
    from public.restaurants2
),
legacy_details as (
    select *
    from public.restaurant_details
),
prepared as (
    select
        l.place_id,
        coalesce(r2.id, 'gm_' || l.place_id) as proposed_id,
        case when r2.id is not null then true else false end as reuses_legacy_id,
        case when r2.id is null then true else false end as generates_fallback_id,
        coalesce(nullif(l.title, ''), 'Unnamed place ' || l.place_id) as proposed_name,
        nullif(l.category_name, '') as proposed_category_name,
        nullif(l.address, '') as proposed_address,
        coalesce(nullif(l.city, ''), nullif(r2.location, '')) as proposed_city,
        coalesce(nullif(l.website, ''), nullif(rd.restaurant_website, '')) as proposed_website,
        coalesce(nullif(l.url, ''), nullif(rd.google_maps_link, '')) as proposed_google_maps_url,
        nullif(l.image_url, '') as proposed_image_url,
        coalesce(nullif(l.description, ''), nullif(rd.description, ''), ''::text) as proposed_description,
        coalesce(nullif(l.price, ''), nullif(r2.price, ''), nullif(rd.price_range, '')) as proposed_price,
        coalesce(nullif(r2.price, ''), nullif(rd.price_range, ''), nullif(l.price, '')) as proposed_price_range,
        coalesce(l.total_score, r2.rating, rd.rating::double precision) as proposed_rating,
        l.reviews_count as proposed_reviews_count,
        case
            when coalesce(l.location ->> 'lat', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'lat')::double precision
            when coalesce(l.location ->> 'latitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'latitude')::double precision
            else null
        end as raw_extracted_latitude,
        case
            when coalesce(l.location ->> 'lng', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'lng')::double precision
            when coalesce(l.location ->> 'longitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                then (l.location ->> 'longitude')::double precision
            else null
        end as raw_extracted_longitude,
        coalesce(
            case
                when coalesce(l.location ->> 'lat', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lat')::double precision
                when coalesce(l.location ->> 'latitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'latitude')::double precision
                else null
            end,
            r2.latitude
        ) as proposed_latitude,
        coalesce(
            case
                when coalesce(l.location ->> 'lng', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lng')::double precision
                when coalesce(l.location ->> 'longitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'longitude')::double precision
                else null
            end,
            r2.longitude
        ) as proposed_longitude,
        case
            when l.opening_hours is null then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'null' then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'array'
                 and l.opening_hours::jsonb = '[]'::jsonb then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'string'
                 and btrim(l.opening_hours::jsonb #>> '{}') = '' then null
            else l.opening_hours::jsonb
        end as proposed_opening_hours,
        nullif(btrim(l.reserve_table_url), '') as proposed_reserve_table_url,
        case
            when l.order_online is null then null
            when jsonb_typeof(l.order_online::jsonb) = 'null' then null
            when jsonb_typeof(l.order_online::jsonb) = 'array'
                 and l.order_online::jsonb = '[]'::jsonb then null
            when jsonb_typeof(l.order_online::jsonb) = 'string'
                 and btrim(l.order_online::jsonb #>> '{}') = '' then null
            else l.order_online::jsonb
        end as proposed_order_online,
        coalesce(l.permanently_closed, false) as proposed_permanently_closed,
        coalesce(l.temporarily_closed, false) as proposed_temporarily_closed,
        nullif(r2.eta, '') as proposed_eta,
        nullif(rd.distance, '') as proposed_distance,
        coalesce(r2.tags, '[]'::jsonb) as proposed_tags,
        coalesce(
            rd.attributes,
            case
                when nullif(l.category_name, '') is not null then jsonb_build_array(l.category_name)
                else '[]'::jsonb
            end
        ) as proposed_attributes,
        coalesce(r2.review_tag_profile, '{}'::jsonb) as proposed_review_tag_profile,
        coalesce(nullif(r2.vegan_level, ''), 'UNKNOWN') as proposed_vegan_level,
        coalesce(nullif(r2.vegetarian_level, ''), 'UNKNOWN') as proposed_vegetarian_level,
        coalesce(nullif(r2.halal_level, ''), 'UNKNOWN') as proposed_halal_level,
        coalesce(nullif(r2.gluten_free_level, ''), 'UNKNOWN') as proposed_gluten_free_level,
        coalesce(nullif(r2.dairy_free_level, ''), 'UNKNOWN') as proposed_dairy_free_level,
        l.scraped_at as proposed_source_scraped_at,
        l.title as raw_title,
        l.category_name as raw_category_name,
        l.address as raw_address,
        l.city as raw_city,
        l.total_score as raw_total_score,
        l.reviews_count as raw_reviews_count
    from latest_raw l
    left join legacy_restaurants r2
        on r2.id = 'gm_' || l.place_id
    left join legacy_details rd
        on rd.restaurant_id = coalesce(r2.id, 'gm_' || l.place_id)
)
select
    count(*) as expected_app_rows,
    count(*) filter (where nullif(proposed_id, '') is null) as missing_proposed_id,
    count(*) filter (where nullif(place_id, '') is null) as missing_place_id,
    count(*) filter (where nullif(proposed_name, '') is null) as missing_name,
    count(*) filter (where nullif(proposed_category_name, '') is null) as missing_category_name,
    count(*) filter (where nullif(proposed_city, '') is null) as missing_city,
    count(*) filter (where nullif(proposed_address, '') is null) as missing_address,
    count(*) filter (where proposed_rating is null) as missing_rating,
    count(*) filter (where nullif(proposed_price, '') is null) as missing_price,
    count(*) filter (where proposed_latitude is null or proposed_longitude is null) as missing_coordinates,
    round(100.0 * count(*) filter (where nullif(proposed_eta, '') is not null) / nullif(count(*), 0), 2) as eta_coverage_pct,
    round(100.0 * count(*) filter (where nullif(proposed_distance, '') is not null) / nullif(count(*), 0), 2) as distance_coverage_pct,
    round(100.0 * count(*) filter (where nullif(proposed_price_range, '') is not null) / nullif(count(*), 0), 2) as price_range_coverage_pct,
    round(100.0 * count(*) filter (where jsonb_array_length(proposed_tags) > 0) / nullif(count(*), 0), 2) as tags_coverage_pct,
    round(100.0 * count(*) filter (where jsonb_array_length(proposed_attributes) > 0) / nullif(count(*), 0), 2) as attributes_coverage_pct,
    round(100.0 * count(*) filter (where proposed_review_tag_profile <> '{}'::jsonb) / nullif(count(*), 0), 2) as review_tag_profile_coverage_pct,
    round(100.0 * count(*) filter (where proposed_vegan_level <> 'UNKNOWN') / nullif(count(*), 0), 2) as vegan_level_coverage_pct,
    round(100.0 * count(*) filter (where proposed_vegetarian_level <> 'UNKNOWN') / nullif(count(*), 0), 2) as vegetarian_level_coverage_pct,
    round(100.0 * count(*) filter (where proposed_halal_level <> 'UNKNOWN') / nullif(count(*), 0), 2) as halal_level_coverage_pct,
    round(100.0 * count(*) filter (where proposed_gluten_free_level <> 'UNKNOWN') / nullif(count(*), 0), 2) as gluten_free_level_coverage_pct,
    round(100.0 * count(*) filter (where proposed_dairy_free_level <> 'UNKNOWN') / nullif(count(*), 0), 2) as dairy_free_level_coverage_pct,
    count(*) filter (where raw_extracted_latitude is not null and raw_extracted_longitude is not null) as raw_coordinate_pair_extracted_count,
    round(100.0 * count(*) filter (where raw_extracted_latitude is not null and raw_extracted_longitude is not null) / nullif(count(*), 0), 2) as raw_coordinate_pair_extraction_pct,
    count(*) filter (where proposed_latitude is not null and proposed_longitude is not null) as final_coordinate_pair_available_count,
    round(100.0 * count(*) filter (where proposed_latitude is not null and proposed_longitude is not null) / nullif(count(*), 0), 2) as final_coordinate_pair_availability_pct
from prepared;

-- ============================================================
-- BLOCK 3: Sample legacy rows not mappable to raw place_id
-- ============================================================
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
),
latest_raw as (
    select *
    from ranked
    where rn = 1
),
legacy_restaurants as (
    select *
    from public.restaurants2
),
legacy_unmapped as (
    select r2.*
    from legacy_restaurants r2
    left join latest_raw l
        on r2.id = 'gm_' || l.place_id
    where l.place_id is null
)
select
    id,
    name,
    category,
    location,
    price,
    eta,
    rating
from legacy_unmapped
order by id
limit 25;

-- ============================================================
-- BLOCK 4: Sample mismatches between latest raw and proposed app rows
-- ============================================================
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
),
latest_raw as (
    select *
    from ranked
    where rn = 1
),
legacy_restaurants as (
    select *
    from public.restaurants2
),
legacy_details as (
    select *
    from public.restaurant_details
),
prepared as (
    select
        l.place_id,
        coalesce(r2.id, 'gm_' || l.place_id) as proposed_id,
        coalesce(nullif(l.title, ''), 'Unnamed place ' || l.place_id) as proposed_name,
        nullif(l.category_name, '') as proposed_category_name,
        nullif(l.address, '') as proposed_address,
        coalesce(nullif(l.city, ''), nullif(r2.location, '')) as proposed_city,
        coalesce(nullif(l.price, ''), nullif(r2.price, ''), nullif(rd.price_range, '')) as proposed_price,
        coalesce(nullif(r2.price, ''), nullif(rd.price_range, ''), nullif(l.price, '')) as proposed_price_range,
        coalesce(l.total_score, r2.rating, rd.rating::double precision) as proposed_rating,
        l.reviews_count as proposed_reviews_count,
        nullif(r2.eta, '') as proposed_eta,
        nullif(rd.distance, '') as proposed_distance,
        coalesce(r2.tags, '[]'::jsonb) as proposed_tags,
        coalesce(
            rd.attributes,
            case
                when nullif(l.category_name, '') is not null then jsonb_build_array(l.category_name)
                else '[]'::jsonb
            end
        ) as proposed_attributes,
        coalesce(r2.review_tag_profile, '{}'::jsonb) as proposed_review_tag_profile,
        l.scraped_at as proposed_source_scraped_at,
        l.title as raw_title,
        l.category_name as raw_category_name,
        l.address as raw_address,
        l.city as raw_city,
        l.total_score as raw_total_score,
        l.reviews_count as raw_reviews_count
    from latest_raw l
    left join legacy_restaurants r2
        on r2.id = 'gm_' || l.place_id
    left join legacy_details rd
        on rd.restaurant_id = coalesce(r2.id, 'gm_' || l.place_id)
)
select
    proposed_id as id,
    place_id,
    proposed_name,
    raw_title,
    proposed_category_name,
    raw_category_name,
    proposed_address,
    raw_address,
    proposed_city,
    raw_city,
    proposed_rating,
    raw_total_score,
    proposed_reviews_count,
    raw_reviews_count,
    proposed_eta,
    proposed_distance,
    proposed_price,
    proposed_price_range,
    proposed_tags,
    proposed_attributes,
    proposed_review_tag_profile,
    proposed_source_scraped_at
from prepared
where coalesce(proposed_name, '') <> coalesce(raw_title, '')
   or coalesce(proposed_category_name, '') <> coalesce(raw_category_name, '')
   or coalesce(proposed_address, '') <> coalesce(raw_address, '')
   or coalesce(proposed_city, '') <> coalesce(raw_city, '')
   or coalesce(proposed_rating, -1) <> coalesce(raw_total_score, -1)
   or coalesce(proposed_reviews_count, -1) <> coalesce(raw_reviews_count, -1)
order by proposed_source_scraped_at desc nulls last
limit 25;

-- ============================================================
-- BLOCK 5: Sample proposed composed rows for manual inspection
-- ============================================================
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
),
latest_raw as (
    select *
    from ranked
    where rn = 1
),
legacy_restaurants as (
    select *
    from public.restaurants2
),
legacy_details as (
    select *
    from public.restaurant_details
),
prepared as (
    select
        l.place_id,
        coalesce(r2.id, 'gm_' || l.place_id) as proposed_id,
        coalesce(nullif(l.title, ''), 'Unnamed place ' || l.place_id) as proposed_name,
        nullif(l.category_name, '') as proposed_category_name,
        nullif(l.address, '') as proposed_address,
        coalesce(nullif(l.city, ''), nullif(r2.location, '')) as proposed_city,
        coalesce(nullif(l.website, ''), nullif(rd.restaurant_website, '')) as proposed_website,
        coalesce(nullif(l.url, ''), nullif(rd.google_maps_link, '')) as proposed_google_maps_url,
        coalesce(nullif(l.price, ''), nullif(r2.price, ''), nullif(rd.price_range, '')) as proposed_price,
        coalesce(nullif(r2.price, ''), nullif(rd.price_range, ''), nullif(l.price, '')) as proposed_price_range,
        coalesce(l.total_score, r2.rating, rd.rating::double precision) as proposed_rating,
        l.reviews_count as proposed_reviews_count,
        coalesce(
            case
                when coalesce(l.location ->> 'lat', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lat')::double precision
                when coalesce(l.location ->> 'latitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'latitude')::double precision
                else null
            end,
            r2.latitude
        ) as proposed_latitude,
        coalesce(
            case
                when coalesce(l.location ->> 'lng', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lng')::double precision
                when coalesce(l.location ->> 'longitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'longitude')::double precision
                else null
            end,
            r2.longitude
        ) as proposed_longitude,
        nullif(r2.eta, '') as proposed_eta,
        nullif(rd.distance, '') as proposed_distance,
        coalesce(r2.tags, '[]'::jsonb) as proposed_tags,
        coalesce(
            rd.attributes,
            case
                when nullif(l.category_name, '') is not null then jsonb_build_array(l.category_name)
                else '[]'::jsonb
            end
        ) as proposed_attributes,
        coalesce(r2.review_tag_profile, '{}'::jsonb) as proposed_review_tag_profile,
        l.scraped_at as proposed_source_scraped_at
    from latest_raw l
    left join legacy_restaurants r2
        on r2.id = 'gm_' || l.place_id
    left join legacy_details rd
        on rd.restaurant_id = coalesce(r2.id, 'gm_' || l.place_id)
)
select
    proposed_id as id,
    place_id,
    proposed_name as name,
    proposed_category_name as category_name,
    proposed_address as address,
    proposed_city as city,
    proposed_website as website,
    proposed_google_maps_url as google_maps_url,
    proposed_price as price,
    proposed_price_range as price_range,
    proposed_rating as rating,
    proposed_reviews_count as reviews_count,
    proposed_latitude as latitude,
    proposed_longitude as longitude,
    proposed_eta as eta,
    proposed_distance as distance,
    proposed_tags as tags,
    proposed_attributes as attributes,
    proposed_review_tag_profile as review_tag_profile,
    proposed_source_scraped_at as source_scraped_at
from prepared
order by proposed_source_scraped_at desc nulls last
limit 25;
