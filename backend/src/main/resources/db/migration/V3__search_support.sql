-- Lets search ignore diacritics, so "Manastur" also finds "Mănăștur"
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Columns used by the list filters
CREATE INDEX idx_announcement_type ON announcement (type);
CREATE INDEX idx_announcement_species ON announcement (species);
CREATE INDEX idx_announcement_status ON announcement (status);

-- Test announcements from V2 that had no map position, matched by their placeholder phone
UPDATE announcement SET latitude = 46.7690, longitude = 23.5900 WHERE contact_phone = '0700000006'; -- Centru
UPDATE announcement SET latitude = 46.7620, longitude = 23.6080 WHERE contact_phone = '0700000009'; -- Andrei Mureșanu
UPDATE announcement SET latitude = 46.7880, longitude = 23.6510 WHERE contact_phone = '0700000013'; -- Someșeni

-- Every area follows the "Neighborhood, City" format
UPDATE announcement SET area = 'Centru, Florești', latitude = 46.7445, longitude = 23.4850 WHERE contact_phone = '0700000012';
UPDATE announcement SET area = 'Centru, Baciu', latitude = 46.7900, longitude = 23.5275 WHERE contact_phone = '0700000014';

-- More fictional test data for search: several cities, colors spelled in different ways and
-- two areas left without diacritics on purpose. Dates are relative to when this migration
-- runs, so they stay recent and are never in the future.
INSERT INTO announcement (type, species, pet_name, breed, color, distinctive_marks, event_date, latitude, longitude, area,
                          contact_name, contact_phone, status, created_at)
VALUES
    ('LOST', 'CAT', 'Pufi', 'Persian', 'grey', 'Long fluffy coat and a flat face, wears a pink collar with a bell',
     CURRENT_DATE - 2 + TIME '19:30', 45.6405, 25.6270, 'Astra, Brașov',
     'Monica Barbu', '0700000015', 'ACTIVE', CURRENT_DATE - 2 + TIME '21:10'),

    ('FOUND', 'DOG', NULL, 'Schnauzer', 'dark grey', 'Bushy beard, worn leather collar without a tag, calm around children',
     CURRENT_DATE - 5 + TIME '08:20', 45.6710, 25.6010, 'Tractorul, Brașov',
     'Sergiu Costea', '0700000016', 'ACTIVE', CURRENT_DATE - 5 + TIME '09:45'),

    ('LOST', 'DOG', 'Rocky', 'Weimaraner', 'Gray', 'Amber eyes, brown collar, runs away from strangers',
     CURRENT_DATE - 9 + TIME '17:15', 45.7560, 21.2490, 'Fabric, Timișoara',
     'Adela Nistor', '0700000017', 'ACTIVE', CURRENT_DATE - 9 + TIME '18:40'),

    ('FOUND', 'CAT', NULL, 'European', 'Black and white', 'White patch on the chest, friendly, seems used to people',
     CURRENT_DATE - 3 + TIME '13:00', 45.7450, 21.2080, 'Iosefin, Timisoara',
     'Tudor Vlaicu', '0700000018', 'ACTIVE', CURRENT_DATE - 3 + TIME '13:50'),

    ('LOST', 'OTHER', 'Coco', 'Cockatiel', 'Grey with a yellow crest', 'Orange cheeks, whistles when it hears its name',
     CURRENT_DATE - 12 + TIME '10:30', 47.1795, 27.5660, 'Copou, Iași',
     'Raluca Sandu', '0700000019', 'ACTIVE', CURRENT_DATE - 12 + TIME '12:05'),

    ('FOUND', 'OTHER', NULL, 'Domestic rabbit', 'Light brown', 'Floppy ears, tame, found in a courtyard between the blocks',
     CURRENT_DATE - 1 + TIME '16:40', 47.1650, 27.6080, 'Tatarasi, Iasi',
     'Bogdan Chiriac', '0700000020', 'ACTIVE', CURRENT_DATE - 1 + TIME '17:25'),

    ('LOST', 'CAT', 'Mia', 'Maine Coon', 'Brown tabby', 'Large with tufted ears, microchipped, already back home',
     CURRENT_DATE - 16 + TIME '22:00', 45.7715, 24.1450, 'Hipodrom, Sibiu',
     'Carmen Oprea', '0700000021', 'RESOLVED', CURRENT_DATE - 15 + TIME '08:30'),

    ('FOUND', 'DOG', NULL, 'Jack Russell Terrier', 'White and tan', 'Small and energetic, red harness, brown patch over one eye',
     CURRENT_DATE - 7 + TIME '11:10', 47.0770, 21.9160, 'Rogerius, Oradea',
     'Ovidiu Pintea', '0700000022', 'ACTIVE', CURRENT_DATE - 7 + TIME '12:00');
