ALTER TABLE vehicules ADD COLUMN IF NOT EXISTS fin_validite_patente DATE;
ALTER TABLE vehicules ADD COLUMN IF NOT EXISTS fin_validite_carte_stationnement DATE;
ALTER TABLE vehicules ADD COLUMN IF NOT EXISTS fin_validite_carte_transport DATE;
