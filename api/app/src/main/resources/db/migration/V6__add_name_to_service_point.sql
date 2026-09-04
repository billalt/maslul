-- Council registries name locations, not just address them (e.g. "Old City Underground Bin").
ALTER TABLE service_point ADD COLUMN name VARCHAR(255);
