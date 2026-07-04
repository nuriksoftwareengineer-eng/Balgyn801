-- Base colors
INSERT INTO colors (name, hex_code) VALUES
  ('Black',  '#000000'),
  ('White',  '#FFFFFF'),
  ('Navy',   '#1B2A4A'),
  ('Red',    '#C0392B'),
  ('Olive',  '#4B5320'),
  ('Gray',   '#6B7280')
ON CONFLICT DO NOTHING;

-- Base sizes
INSERT INTO sizes (label) VALUES ('S'),('M'),('L'),('XL'),('XXL')
ON CONFLICT DO NOTHING;
