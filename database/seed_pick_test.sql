-- Seed data for validating Pick flow with live tables.
-- Idempotent via ON CONFLICT so it can be re-run safely.

begin;

-- Core users (current user + friends with their own preferences/restrictions)
insert into public.users (
    id, username, email, password, bio, preferences, food_restrictions,
    latitude, longitude, notifications_enabled
) values
    (
        'test-user-1', 'Kevin', 'k389zhan@uwaterloo.ca', '12345', '',
        '["pizza","bubble tea","shawarma","coffee"]'::jsonb,
        '["seafood","pork"]'::jsonb,
        43.4723, -80.5449, true
    ),
    (
        'friend-alex', 'Alex', 'alex@example.com', 'alex123', 'Loves noodles',
        '["sushi","ramen","tea"]'::jsonb,
        '["dairy"]'::jsonb,
        43.4516, -80.4925, true
    ),
    (
        'friend-sally', 'Sally', 'sally@example.com', 'sally123', 'Veggie first',
        '["vegetarian","salad","coffee"]'::jsonb,
        '["gluten"]'::jsonb,
        43.4516, -80.4925, true
    ),
    (
        'friend-mandy', 'Mandy', 'mandy@example.com', 'mandy123', 'Burger fan',
        '["burgers","fries","fast food"]'::jsonb,
        '[]'::jsonb,
        43.4680, -80.5200, true
    )
on conflict (id) do update set
    username = excluded.username,
    email = excluded.email,
    password = excluded.password,
    bio = excluded.bio,
    preferences = excluded.preferences,
    food_restrictions = excluded.food_restrictions,
    latitude = excluded.latitude,
    longitude = excluded.longitude,
    notifications_enabled = excluded.notifications_enabled,
    modified_at = now();

-- Friends list for test-user-1 (friend ids match user ids)
insert into public.friends (id, user_id, name)
values
    ('friend-alex', 'test-user-1', 'Alex'),
    ('friend-sally', 'test-user-1', 'Sally'),
    ('friend-mandy', 'test-user-1', 'Mandy')
on conflict (id) do update set
    user_id = excluded.user_id,
    name = excluded.name,
    modified_at = now();

-- Restaurants (mix of locations, prices, ratings, open/closed)
insert into public.restaurants2 (
    id, name, category, price, eta, rating, location, is_open_now
) values
    ('r_pizza_1', 'Waterloo Brick Oven Pizza', 'Pizza', '$14.99', '18-28 min', 4.7, 'Waterloo', true),
    ('r_shawarma_1', 'Cedar Shawarma House', 'Middle Eastern', '$11.25', '12-20 min', 4.6, 'Waterloo', true),
    ('r_bubble_1', 'The Alley Bubble Tea', 'Drink', '$6.50', '8-15 min', 4.4, 'Waterloo', true),
    ('r_sushi_1', 'Harbor Sushi Bar', 'Sushi', '$21.00', '25-35 min', 4.8, 'Kitchener', true),
    ('r_ramen_1', 'Pork Belly Ramen', 'Japanese', '$17.50', '18-26 min', 4.6, 'Waterloo', true),
    ('r_cafe_1', 'Maple Leaf Cafe', 'Coffee', '$4.95', '6-12 min', 4.2, 'Waterloo', false),
    ('r_veggie_1', 'Kitchener Veggie Bowl', 'Local', '$13.25', '20-30 min', 4.1, 'Kitchener', true),
    ('r_steak_1', 'Toronto Prime Steakhouse', 'Grill', '$42.00', '35-50 min', 4.9, 'Toronto', true),
    ('r_burger_1', 'Campus Burger & Fries', 'Burgers', '$9.75', '14-22 min', 4.0, 'Waterloo', true),
    ('r_pasta_1', 'Gluten-Free Pasta Place', 'Italian', '$15.50', '18-25 min', 4.3, 'Waterloo', true)
on conflict (id) do update set
    name = excluded.name,
    category = excluded.category,
    price = excluded.price,
    eta = excluded.eta,
    rating = excluded.rating,
    location = excluded.location,
    is_open_now = excluded.is_open_now;

-- Restaurant details
insert into public.restaurant_details (
    restaurant_id, name, description, rating, price_range,
    images, attributes, google_maps_link, restaurant_website,
    city, address, distance
) values
    (
        'r_pizza_1', 'Waterloo Brick Oven Pizza',
        'Wood-fired pizza with sourdough crust and house mozzarella.',
        4.7, '$$',
        '["pizza_1","pizza_2"]'::jsonb,
        '["Pizza","Wood-Fired","Family Friendly"]'::jsonb,
        'https://maps.example/pizza', 'https://pizza.example',
        'Waterloo', '123 King St N, Waterloo', '1.2 km'
    ),
    (
        'r_shawarma_1', 'Cedar Shawarma House',
        'Halal shawarma platters with garlic sauce and pickles.',
        4.6, '$',
        '["shawarma_1","shawarma_2"]'::jsonb,
        '["Halal","Middle Eastern","Late Night"]'::jsonb,
        'https://maps.example/shawarma', 'https://shawarma.example',
        'Waterloo', '44 Columbia St W, Waterloo', '0.9 km'
    ),
    (
        'r_bubble_1', 'The Alley Bubble Tea',
        'Brown sugar milk tea and classic bubble tea with chewy pearls.',
        4.4, '$',
        '["bubble_1","bubble_2"]'::jsonb,
        '["Bubble Tea","Dessert Drinks","Takeout"]'::jsonb,
        'https://maps.example/bubble', 'https://bubble.example',
        'Waterloo', '10 King St S, Waterloo', '0.7 km'
    ),
    (
        'r_sushi_1', 'Harbor Sushi Bar',
        'Fresh sushi, sashimi, and seafood rolls prepared daily.',
        4.8, '$$',
        '["sushi_1","sushi_2"]'::jsonb,
        '["Sushi","Seafood","Japanese"]'::jsonb,
        'https://maps.example/sushi', 'https://sushi.example',
        'Kitchener', '77 Queen St N, Kitchener', '4.4 km'
    ),
    (
        'r_ramen_1', 'Pork Belly Ramen',
        'Rich pork broth ramen with slow-cooked pork belly.',
        4.6, '$$',
        '["ramen_1","ramen_2"]'::jsonb,
        '["Ramen","Pork","Japanese"]'::jsonb,
        'https://maps.example/ramen', 'https://ramen.example',
        'Waterloo', '6 University Ave W, Waterloo', '1.5 km'
    ),
    (
        'r_cafe_1', 'Maple Leaf Cafe',
        'All-day espresso bar and pour-over coffee.',
        4.2, '$',
        '["cafe_1","cafe_2"]'::jsonb,
        '["Coffee","Cafe","Study Spot"]'::jsonb,
        'https://maps.example/cafe', 'https://cafe.example',
        'Waterloo', '205 Lester St, Waterloo', '1.1 km'
    ),
    (
        'r_veggie_1', 'Kitchener Veggie Bowl',
        'Seasonal veggie bowls with grains and roasted vegetables.',
        4.1, '$$',
        '["veggie_1"]'::jsonb,
        '["Vegetarian","Healthy","Bowl"]'::jsonb,
        'https://maps.example/veggie', 'https://veggie.example',
        'Kitchener', '88 Weber St E, Kitchener', '4.8 km'
    ),
    (
        'r_steak_1', 'Toronto Prime Steakhouse',
        'Dry-aged steakhouse with premium cuts and seafood sides.',
        4.9, '$$$',
        '["steak_1"]'::jsonb,
        '["Steak","Grill","Fine Dining"]'::jsonb,
        'https://maps.example/steak', 'https://steak.example',
        'Toronto', '1 King St W, Toronto', '110 km'
    ),
    (
        'r_burger_1', 'Campus Burger & Fries',
        'Classic burgers, fries, and house dipping sauces.',
        4.0, '$',
        '["burger_1"]'::jsonb,
        '["Burgers","Fast Food","Late Night"]'::jsonb,
        'https://maps.example/burger', 'https://burger.example',
        'Waterloo', '15 Columbia St E, Waterloo', '0.6 km'
    ),
    (
        'r_pasta_1', 'Gluten-Free Pasta Place',
        'Gluten-free pasta bowls with house tomato sauce.',
        4.3, '$$',
        '["pasta_1"]'::jsonb,
        '["Italian","Pasta","Gluten Free"]'::jsonb,
        'https://maps.example/pasta', 'https://pasta.example',
        'Waterloo', '300 King St N, Waterloo', '2.0 km'
    )
on conflict (restaurant_id) do update set
    name = excluded.name,
    description = excluded.description,
    rating = excluded.rating,
    price_range = excluded.price_range,
    images = excluded.images,
    attributes = excluded.attributes,
    google_maps_link = excluded.google_maps_link,
    restaurant_website = excluded.restaurant_website,
    city = excluded.city,
    address = excluded.address,
    distance = excluded.distance;

-- Featured items
insert into public.featured_items (
    id, restaurant_id, name, price, description
) values
    ('fi_pizza_1', 'r_pizza_1', 'Margherita Pizza', '$13.50', 'Fresh basil, tomato, mozzarella'),
    ('fi_pizza_2', 'r_pizza_1', 'Truffle Mushroom Pizza', '$16.00', 'Mushrooms, truffle oil, parmesan'),
    ('fi_shawarma_1', 'r_shawarma_1', 'Chicken Shawarma Plate', '$12.25', 'Rice, garlic sauce, pickles'),
    ('fi_shawarma_2', 'r_shawarma_1', 'Beef Shawarma Wrap', '$11.00', 'Wrap with fries and sauce'),
    ('fi_bubble_1', 'r_bubble_1', 'Brown Sugar Milk Tea', '$6.90', 'Signature drink with pearls'),
    ('fi_bubble_2', 'r_bubble_1', 'Royal No.9 Milk Tea', '$6.20', 'House black tea blend'),
    ('fi_sushi_1', 'r_sushi_1', 'Salmon Nigiri Set', '$19.00', 'Fresh salmon and rice'),
    ('fi_ramen_1', 'r_ramen_1', 'Tonkotsu Ramen', '$17.50', 'Pork broth, chashu, egg'),
    ('fi_cafe_1', 'r_cafe_1', 'Maple Latte', '$4.95', 'Espresso with maple'),
    ('fi_burger_1', 'r_burger_1', 'Double Cheeseburger', '$9.75', 'Two patties, cheese, fries'),
    ('fi_pasta_1', 'r_pasta_1', 'Tomato Basil Pasta', '$15.50', 'Gluten-free penne')
on conflict (id) do update set
    restaurant_id = excluded.restaurant_id,
    name = excluded.name,
    price = excluded.price,
    description = excluded.description,
    modified_at = now();

-- Saved restaurants (used for saved list and to seed new vote sessions)
insert into public.user_saved_restaurants (user_id, restaurant_id)
values
    ('test-user-1', 'r_pizza_1'),
    ('test-user-1', 'r_bubble_1'),
    ('friend-alex', 'r_shawarma_1'),
    ('friend-alex', 'r_bubble_1'),
    ('friend-sally', 'r_pizza_1'),
    ('friend-sally', 'r_veggie_1'),
    ('friend-mandy', 'r_burger_1'),
    ('friend-mandy', 'r_shawarma_1')
on conflict (user_id, restaurant_id) do update set
    modified_at = now();

-- Sample vote session (used by Pick group voting screen)
insert into public.vote_sessions (id, title, created_at_epoch, is_closed, closed_at)
values
    ('vote_seed_1', 'Vote with Alex, Sally', 1710000000000, false, null)
on conflict (id) do update set
    title = excluded.title,
    created_at_epoch = excluded.created_at_epoch,
    is_closed = excluded.is_closed,
    closed_at = excluded.closed_at;

insert into public.vote_session_participants (session_id, user_id, display_name)
values
    ('vote_seed_1', 'test-user-1', 'Kevin'),
    ('vote_seed_1', 'friend-alex', 'Alex'),
    ('vote_seed_1', 'friend-sally', 'Sally')
on conflict (session_id, user_id) do update set
    display_name = excluded.display_name;

insert into public.vote_session_restaurants (session_id, restaurant_id)
values
    ('vote_seed_1', 'r_pizza_1'),
    ('vote_seed_1', 'r_shawarma_1'),
    ('vote_seed_1', 'r_bubble_1'),
    ('vote_seed_1', 'r_veggie_1')
on conflict (session_id, restaurant_id) do nothing;

insert into public.vote_session_votes (session_id, user_id, restaurant_id)
values
    ('vote_seed_1', 'test-user-1', 'r_pizza_1'),
    ('vote_seed_1', 'test-user-1', 'r_bubble_1'),
    ('vote_seed_1', 'friend-alex', 'r_shawarma_1'),
    ('vote_seed_1', 'friend-alex', 'r_bubble_1'),
    ('vote_seed_1', 'friend-sally', 'r_pizza_1'),
    ('vote_seed_1', 'friend-sally', 'r_veggie_1')
on conflict (session_id, user_id, restaurant_id) do nothing;

commit;
