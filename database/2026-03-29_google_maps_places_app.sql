-- REVIEW ONLY.
-- This script is intentionally non-destructive:
-- 1) it creates a new app-facing read model table;
-- 2) it backfills that table from the raw ingestion table plus legacy app tables;
-- 3) it does not drop or alter any existing table or FK.
--
-- Model intent:
-- `public.google_maps_places_app` is a COMPOSED APP READ MODEL.
-- It is not a pure mirror of `public.google_maps_places_full`.
-- Raw fields come from `google_maps_places_full`.
-- Legacy app-specific fields that the UI/recommendation logic still needs
-- (eta, distance, price_range, tags, attributes, review_tag_profile, dietary levels)
-- are preserved from `restaurants2` and `restaurant_details`.

begin;

create table if not exists public.google_maps_places_app (
    -- Stable app-facing ID. Preserve the existing legacy restaurant ID when available.
    -- If no legacy ID exists for a place, generate `gm_<place_id>` as a deterministic fallback.
    id text primary key,

    -- Raw Google place identifier. One current-state row per place_id.
    place_id text not null unique,

    -- Current UI-facing identity and list fields.
    name text not null,
    category_name text null,
    address text null,
    city text null,

    -- Detail-page links and media.
    website text null,
    google_maps_url text null,
    image_url text null,
    description text null default ''::text,

    -- List/detail pricing and rating.
    price text null,
    price_range text null,
    rating double precision null,
    reviews_count bigint null,

    -- Map/distance filtering support.
    latitude double precision null,
    longitude double precision null,

    -- Product-useful but not yet heavily consumed fields.
    opening_hours jsonb null,
    reserve_table_url text null,
    order_online jsonb null,
    permanently_closed boolean not null default false,
    temporarily_closed boolean not null default false,

    -- Legacy UI/supporting fields still used by the current app.
    eta text null,
    distance text null,
    tags jsonb not null default '[]'::jsonb,
    attributes jsonb not null default '[]'::jsonb,
    review_tag_profile jsonb not null default '{}'::jsonb,
    vegan_level text not null default 'UNKNOWN',
    vegetarian_level text not null default 'UNKNOWN',
    halal_level text not null default 'UNKNOWN',
    gluten_free_level text not null default 'UNKNOWN',
    dairy_free_level text not null default 'UNKNOWN',

    -- Provenance / freshness.
    source_scraped_at timestamp with time zone null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
);

create index if not exists google_maps_places_app_name_idx
    on public.google_maps_places_app (lower(name));

create index if not exists google_maps_places_app_city_idx
    on public.google_maps_places_app (lower(city));

create index if not exists google_maps_places_app_category_idx
    on public.google_maps_places_app (lower(category_name));

create index if not exists google_maps_places_app_source_scraped_at_idx
    on public.google_maps_places_app (source_scraped_at desc nulls last);

create index if not exists google_maps_places_app_tags_gin_idx
    on public.google_maps_places_app using gin (tags);

with ranked as (
    select
        g.*,
        row_number() over (
            partition by g.place_id
            order by
                -- Dedupe rule:
                -- choose the newest snapshot first;
                -- if multiple rows share the same timestamp, prefer the richer/more trusted row:
                -- more reviews, then higher score, then deterministic title ordering.
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
prepared as (
    select
        -- Preserve an existing app/legacy ID when possible so favorites, reviews,
        -- and vote-related references can continue to point at the same identifier.
        -- If no legacy row exists, generate a deterministic fallback ID.
        coalesce(r2.id, r2_name.id, 'gm_' || l.place_id) as id,

        -- Raw-derived current-state fields.
        l.place_id,
        coalesce(nullif(l.title, ''), 'Unnamed place ' || l.place_id) as name,
        nullif(l.category_name, '') as category_name,
        nullif(l.address, '') as address,
        coalesce(nullif(l.city, ''), nullif(r2.location, ''), nullif(r2_name.location, '')) as city,
        coalesce(nullif(l.website, ''), nullif(rd.restaurant_website, '')) as website,
        coalesce(nullif(l.url, ''), nullif(rd.google_maps_link, '')) as google_maps_url,
        nullif(l.image_url, '') as image_url,
        coalesce(nullif(l.description, ''), nullif(rd.description, ''), ''::text) as description,
        coalesce(nullif(l.price, ''), nullif(r2.price, ''), nullif(r2_name.price, ''), nullif(rd.price_range, '')) as price,
        coalesce(nullif(r2.price, ''), nullif(r2_name.price, ''), nullif(rd.price_range, ''), nullif(l.price, '')) as price_range,
        coalesce(l.total_score, r2.rating, r2_name.rating, rd.rating::double precision) as rating,
        l.reviews_count as reviews_count,
        coalesce(
            case
                when coalesce(l.location ->> 'lat', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lat')::double precision
                when coalesce(l.location ->> 'latitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'latitude')::double precision
                else null
            end,
            r2.latitude,
            r2_name.latitude
        ) as latitude,
        coalesce(
            case
                when coalesce(l.location ->> 'lng', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'lng')::double precision
                when coalesce(l.location ->> 'longitude', '') ~ '^-?[0-9]+([.][0-9]+)?$'
                    then (l.location ->> 'longitude')::double precision
                else null
            end,
            r2.longitude,
            r2_name.longitude
        ) as longitude,
        case
            when l.opening_hours is null then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'null' then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'array'
                 and l.opening_hours::jsonb = '[]'::jsonb then null
            when jsonb_typeof(l.opening_hours::jsonb) = 'string'
                 and btrim(l.opening_hours::jsonb #>> '{}') = '' then null
            else l.opening_hours::jsonb
        end as opening_hours,
        nullif(l.reserve_table_url, '') as reserve_table_url,
        case
            when l.order_online is null then null
            when jsonb_typeof(l.order_online::jsonb) = 'null' then null
            when jsonb_typeof(l.order_online::jsonb) = 'array'
                 and l.order_online::jsonb = '[]'::jsonb then null
            when jsonb_typeof(l.order_online::jsonb) = 'string'
                 and btrim(l.order_online::jsonb #>> '{}') = '' then null
            else l.order_online::jsonb
        end as order_online,
        coalesce(l.permanently_closed, false) as permanently_closed,
        coalesce(l.temporarily_closed, false) as temporarily_closed,

        -- Legacy-derived compatibility fields that still matter to current UI/recommendation logic.
        coalesce(nullif(r2.eta, ''), nullif(r2_name.eta, ''), 'ETA unavailable') as eta,
        coalesce(nullif(rd.distance, ''), 'Distance unavailable') as distance,
        case
            when coalesce(jsonb_array_length(r2.tags), 0) > 0 then r2.tags
            when coalesce(jsonb_array_length(r2_name.tags), 0) > 0 then r2_name.tags
            when lower(coalesce(l.category_name, '')) like '%pizza%' then '["pizza","italian"]'::jsonb
            when lower(coalesce(l.category_name, '')) like '%coffee%' or lower(coalesce(l.category_name, '')) like '%cafe%' then '["coffee","cafe","drink"]'::jsonb
            when lower(coalesce(l.category_name, '')) like '%barbecue%' or lower(coalesce(l.category_name, '')) like '%burger%' or lower(coalesce(l.category_name, '')) like '%fast food%' then '["fast food","burger"]'::jsonb
            when lower(coalesce(l.category_name, '')) like '%sushi%' then '["sushi","japanese","seafood"]'::jsonb
            when lower(coalesce(l.category_name, '')) like '%indian%' then '["indian","curry"]'::jsonb
            when lower(coalesce(l.category_name, '')) like '%middle eastern%' or lower(coalesce(l.category_name, '')) like '%shawarma%' then '["middle eastern","shawarma","halal"]'::jsonb
            when nullif(l.category_name, '') is not null then jsonb_build_array(lower(l.category_name))
            else '[]'::jsonb
        end as tags,
        coalesce(
            rd.attributes,
            case
                when nullif(l.category_name, '') is not null then jsonb_build_array(l.category_name)
                else '[]'::jsonb
            end
        ) as attributes,
        coalesce(r2.review_tag_profile, r2_name.review_tag_profile, '{}'::jsonb) as review_tag_profile,
        coalesce(
            nullif(r2.vegan_level, ''),
            nullif(r2_name.vegan_level, ''),
            case
                when lower(coalesce(l.category_name, '')) like '%coffee%' or lower(coalesce(l.category_name, '')) like '%cafe%' or lower(coalesce(l.category_name, '')) like '%drink%' then 'PARTIAL'
                when lower(coalesce(l.category_name, '')) like '%pizza%' or lower(coalesce(l.category_name, '')) like '%italian%' or lower(coalesce(l.category_name, '')) like '%indian%' then 'PARTIAL'
                when lower(coalesce(l.category_name, '')) like '%sushi%' or lower(coalesce(l.category_name, '')) like '%seafood%' then 'NONE'
                when lower(coalesce(l.category_name, '')) like '%burger%' or lower(coalesce(l.category_name, '')) like '%fast food%' or lower(coalesce(l.category_name, '')) like '%grill%' then 'NONE'
                when lower(coalesce(l.category_name, '')) like '%middle eastern%' or lower(coalesce(l.category_name, '')) like '%shawarma%' then 'PARTIAL'
                else 'UNKNOWN'
            end
        ) as vegan_level,
        coalesce(
            nullif(r2.vegetarian_level, ''),
            nullif(r2_name.vegetarian_level, ''),
            case
                when lower(coalesce(l.category_name, '')) like '%coffee%' or lower(coalesce(l.category_name, '')) like '%cafe%' or lower(coalesce(l.category_name, '')) like '%drink%' then 'FULL'
                when lower(coalesce(l.category_name, '')) like '%pizza%' or lower(coalesce(l.category_name, '')) like '%italian%' or lower(coalesce(l.category_name, '')) like '%indian%' then 'PARTIAL'
                when lower(coalesce(l.category_name, '')) like '%sushi%' or lower(coalesce(l.category_name, '')) like '%seafood%' then 'PARTIAL'
                when lower(coalesce(l.category_name, '')) like '%burger%' or lower(coalesce(l.category_name, '')) like '%fast food%' or lower(coalesce(l.category_name, '')) like '%grill%' then 'PARTIAL'
                when lower(coalesce(l.category_name, '')) like '%middle eastern%' or lower(coalesce(l.category_name, '')) like '%shawarma%' then 'PARTIAL'
                else 'UNKNOWN'
            end
        ) as vegetarian_level,
        coalesce(
            nullif(r2.halal_level, ''),
            nullif(r2_name.halal_level, ''),
            case
                when lower(coalesce(l.category_name, '')) like '%middle eastern%' or lower(coalesce(l.category_name, '')) like '%shawarma%' then 'PARTIAL'
                else 'UNKNOWN'
            end
        ) as halal_level,
        coalesce(
            nullif(r2.gluten_free_level, ''),
            nullif(r2_name.gluten_free_level, ''),
            case
                when lower(coalesce(l.category_name, '')) like '%coffee%' or lower(coalesce(l.category_name, '')) like '%cafe%' or lower(coalesce(l.category_name, '')) like '%drink%' then 'PARTIAL'
                else 'UNKNOWN'
            end
        ) as gluten_free_level,
        coalesce(
            nullif(r2.dairy_free_level, ''),
            nullif(r2_name.dairy_free_level, ''),
            case
                when lower(coalesce(l.category_name, '')) like '%coffee%' or lower(coalesce(l.category_name, '')) like '%cafe%' or lower(coalesce(l.category_name, '')) like '%drink%' then 'PARTIAL'
                else 'UNKNOWN'
            end
        ) as dairy_free_level,

        -- Raw freshness marker.
        l.scraped_at as source_scraped_at
    from latest_raw l
    left join public.restaurants2 r2
        on r2.id = 'gm_' || l.place_id
    left join lateral (
        select candidate.*
        from public.restaurants2 candidate
        where lower(trim(coalesce(candidate.name, ''))) = lower(trim(coalesce(l.title, '')))
          and lower(trim(coalesce(candidate.location, ''))) = lower(trim(coalesce(l.city, '')))
        order by candidate.id
        limit 1
    ) r2_name on r2.id is null
    left join public.restaurant_details rd
        on rd.restaurant_id = coalesce(r2.id, r2_name.id, 'gm_' || l.place_id)
)
insert into public.google_maps_places_app (
    id,
    place_id,
    name,
    category_name,
    address,
    city,
    website,
    google_maps_url,
    image_url,
    description,
    price,
    price_range,
    rating,
    reviews_count,
    latitude,
    longitude,
    opening_hours,
    reserve_table_url,
    order_online,
    permanently_closed,
    temporarily_closed,
    eta,
    distance,
    tags,
    attributes,
    review_tag_profile,
    vegan_level,
    vegetarian_level,
    halal_level,
    gluten_free_level,
    dairy_free_level,
    source_scraped_at,
    updated_at
)
select
    id,
    place_id,
    name,
    category_name,
    address,
    city,
    website,
    google_maps_url,
    image_url,
    description,
    price,
    price_range,
    rating,
    reviews_count,
    latitude,
    longitude,
    opening_hours,
    reserve_table_url,
    order_online,
    permanently_closed,
    temporarily_closed,
    eta,
    distance,
    tags,
    attributes,
    review_tag_profile,
    vegan_level,
    vegetarian_level,
    halal_level,
    gluten_free_level,
    dairy_free_level,
    source_scraped_at,
    now()
from prepared
on conflict (id) do update
set
    place_id = excluded.place_id,
    name = excluded.name,
    category_name = excluded.category_name,
    address = excluded.address,
    city = excluded.city,
    website = excluded.website,
    google_maps_url = excluded.google_maps_url,
    image_url = excluded.image_url,
    description = excluded.description,
    price = excluded.price,
    price_range = excluded.price_range,
    rating = excluded.rating,
    reviews_count = excluded.reviews_count,
    latitude = excluded.latitude,
    longitude = excluded.longitude,
    opening_hours = excluded.opening_hours,
    reserve_table_url = excluded.reserve_table_url,
    order_online = excluded.order_online,
    permanently_closed = excluded.permanently_closed,
    temporarily_closed = excluded.temporarily_closed,
    eta = excluded.eta,
    distance = excluded.distance,
    tags = excluded.tags,
    attributes = excluded.attributes,
    review_tag_profile = excluded.review_tag_profile,
    vegan_level = excluded.vegan_level,
    vegetarian_level = excluded.vegetarian_level,
    halal_level = excluded.halal_level,
    gluten_free_level = excluded.gluten_free_level,
    dairy_free_level = excluded.dairy_free_level,
    source_scraped_at = excluded.source_scraped_at,
    updated_at = now();

commit;
