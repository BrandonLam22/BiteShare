-- Switch restaurant foreign keys from legacy `restaurants2` to `google_maps_places_app`.
-- Prerequisite: `google_maps_places_app` exists and contains every restaurant_id referenced
-- by user_saved_restaurants, reviews, vote_session_restaurants, and vote_session_votes.

begin;

alter table public.user_saved_restaurants
    drop constraint if exists user_saved_restaurants_restaurant_id_fkey,
    add constraint user_saved_restaurants_restaurant_id_fkey
        foreign key (restaurant_id) references public.google_maps_places_app (id)
        on update cascade on delete cascade;

alter table public.reviews
    drop constraint if exists reviews_restaurant_id_fkey,
    add constraint reviews_restaurant_id_fkey
        foreign key (restaurant_id) references public.google_maps_places_app (id)
        on update cascade on delete set null;

alter table public.vote_session_restaurants
    drop constraint if exists vote_session_restaurants_restaurant_id_fkey,
    add constraint vote_session_restaurants_restaurant_id_fkey
        foreign key (restaurant_id) references public.google_maps_places_app (id)
        on update cascade on delete cascade;

alter table public.vote_session_votes
    drop constraint if exists vote_session_votes_restaurant_id_fkey,
    add constraint vote_session_votes_restaurant_id_fkey
        foreign key (restaurant_id) references public.google_maps_places_app (id)
        on update cascade on delete cascade;

commit;
