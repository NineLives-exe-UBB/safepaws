-- Lets search ignore diacritics, so "Manastur" also finds "Mănăștur"
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Columns used by the list filters
CREATE INDEX idx_announcement_type ON announcement (type);
CREATE INDEX idx_announcement_species ON announcement (species);
CREATE INDEX idx_announcement_status ON announcement (status);
