-- REVIEW ONLY.
-- Rollback for the proposed app read model migration.
-- This rollback assumes:
-- 1) no foreign keys have been repointed to `public.google_maps_places_app`;
-- 2) the legacy tables remain untouched and still back the app.
--
-- If future changes add dependencies on this table, review them before running rollback.

begin;

drop index if exists public.google_maps_places_app_tags_gin_idx;
drop index if exists public.google_maps_places_app_source_scraped_at_idx;
drop index if exists public.google_maps_places_app_category_idx;
drop index if exists public.google_maps_places_app_city_idx;
drop index if exists public.google_maps_places_app_name_idx;

drop table if exists public.google_maps_places_app;

commit;
