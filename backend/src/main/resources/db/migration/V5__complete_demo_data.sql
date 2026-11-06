-- Completes the fictional demo data, so every announcement's details match its photo.
-- Announcements are matched by their placeholder phone.
UPDATE announcement SET distinctive_marks = 'Young kitten with blue eyes, white chest and paws, very playful'
WHERE contact_phone = '0700000001'; -- Mitzi
UPDATE announcement SET distinctive_marks = 'Dark collar with a name tag, slightly limps on the back left leg'
WHERE contact_phone = '0700000002'; -- Rex
UPDATE announcement SET breed = 'European'
WHERE contact_phone = '0700000003';
UPDATE announcement SET color = 'Brown and white', distinctive_marks = 'Medium size, white stripe down the face, white paws, no collar'
WHERE contact_phone = '0700000004';
UPDATE announcement SET distinctive_marks = 'Dark collar without a tag, knows basic commands'
WHERE contact_phone = '0700000007';
UPDATE announcement SET breed = 'Mixed breed', distinctive_marks = 'Young, wiry coat, floppy ears, afraid of loud noises'
WHERE contact_phone = '0700000008'; -- Bobi
UPDATE announcement SET breed = 'European', color = 'Grey tabby and white', distinctive_marks = 'Kitten, around 3 months old, found near the market stalls, very curious'
WHERE contact_phone = '0700000009';
UPDATE announcement SET distinctive_marks = 'Very small, soft white coat, dark eyes and a pink nose'
WHERE contact_phone = '0700000010'; -- Bunny
UPDATE announcement SET distinctive_marks = 'Round face, copper eyes, thin black collar with a small bell'
WHERE contact_phone = '0700000011'; -- Tom
UPDATE announcement SET distinctive_marks = 'Blue eyes, wears a red collar with a metal ring'
WHERE contact_phone = '0700000012';
UPDATE announcement SET distinctive_marks = 'Microchipped, white stripe on the face, brown speckles on the legs'
WHERE contact_phone = '0700000013'; -- Max
UPDATE announcement SET breed = 'Domestic pigeon', distinctive_marks = 'Cannot fly, found resting on the pavement, calm when approached'
WHERE contact_phone = '0700000014';
UPDATE announcement SET breed = 'European', distinctive_marks = 'Green eyes, darker stripes on the legs and tail, no collar'
WHERE contact_phone = '0700000015'; -- Pufi
UPDATE announcement SET distinctive_marks = 'Bushy beard and eyebrows, calm around children'
WHERE contact_phone = '0700000016';
UPDATE announcement SET distinctive_marks = 'Silver coat, long floppy ears, no collar, runs away from strangers'
WHERE contact_phone = '0700000017'; -- Rocky
UPDATE announcement SET distinctive_marks = 'Small and energetic, brown patches over both eyes'
WHERE contact_phone = '0700000022';

-- Dates relative to when this migration runs, like the announcements added in V3,
-- so they stay recent and are never in the future.
UPDATE announcement SET event_date = CURRENT_DATE - 6 + TIME '18:00', created_at = CURRENT_DATE - 6 + TIME '20:15' WHERE contact_phone = '0700000001';
UPDATE announcement SET event_date = CURRENT_DATE - 8 + TIME '07:30', created_at = CURRENT_DATE - 8 + TIME '09:05' WHERE contact_phone = '0700000002';
UPDATE announcement SET event_date = CURRENT_DATE - 4 + TIME '12:00', created_at = CURRENT_DATE - 4 + TIME '13:40' WHERE contact_phone = '0700000003';
UPDATE announcement SET event_date = CURRENT_DATE - 11 + TIME '16:45', created_at = CURRENT_DATE - 11 + TIME '17:30' WHERE contact_phone = '0700000004';
UPDATE announcement SET event_date = CURRENT_DATE - 10 + TIME '10:00', created_at = CURRENT_DATE - 10 + TIME '11:20' WHERE contact_phone = '0700000005';
UPDATE announcement SET event_date = CURRENT_DATE - 14 + TIME '21:00', created_at = CURRENT_DATE - 13 + TIME '08:10' WHERE contact_phone = '0700000006';
UPDATE announcement SET event_date = CURRENT_DATE - 2 + TIME '08:15', created_at = CURRENT_DATE - 2 + TIME '09:00' WHERE contact_phone = '0700000007';
UPDATE announcement SET event_date = CURRENT_DATE - 3 + TIME '19:30', created_at = CURRENT_DATE - 3 + TIME '22:45' WHERE contact_phone = '0700000008';
UPDATE announcement SET event_date = CURRENT_DATE - 5 + TIME '14:00', created_at = CURRENT_DATE - 5 + TIME '15:10' WHERE contact_phone = '0700000009';
UPDATE announcement SET event_date = CURRENT_DATE - 18 + TIME '09:00', created_at = CURRENT_DATE - 18 + TIME '10:30' WHERE contact_phone = '0700000010';
UPDATE announcement SET event_date = CURRENT_DATE - 1 + TIME '17:00', created_at = CURRENT_DATE - 1 + TIME '18:25' WHERE contact_phone = '0700000011';
UPDATE announcement SET event_date = CURRENT_DATE - 20 + TIME '11:30', created_at = CURRENT_DATE - 20 + TIME '12:00' WHERE contact_phone = '0700000012';
UPDATE announcement SET event_date = CURRENT_DATE - 4 + TIME '07:00', created_at = CURRENT_DATE - 4 + TIME '07:45' WHERE contact_phone = '0700000013';
UPDATE announcement SET event_date = CURRENT_DATE - 13 + TIME '13:00', created_at = CURRENT_DATE - 13 + TIME '13:20' WHERE contact_phone = '0700000014';
