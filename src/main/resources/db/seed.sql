INSERT INTO inventory_items
    (part_number, name, category, storage_location, quantity, reorder_level, description)
VALUES
    ('PCB-CTRL-100', 'Control Board', 'Circuit Board', 'A-01-01', 12, 4, 'General-purpose controller board'),
    ('PCB-PWR-220', 'Power Distribution Board', 'Circuit Board', 'A-01-02', 6, 3, 'Four-channel DC power distribution'),
    ('CBL-USB-C-2M', 'USB-C Cable 2 m', 'Cable', 'B-03-04', 48, 15, 'Shielded data and power cable'),
    ('CBL-RJ45-5M', 'Ethernet Cable 5 m', 'Cable', 'B-03-05', 22, 10, 'Cat6 patch cable'),
    ('WIRE-22-BLK', '22 AWG Black Wire', 'Wire', 'B-05-01', 300, 100, 'Stranded copper wire measured in metres'),
    ('WIRE-22-RED', '22 AWG Red Wire', 'Wire', 'B-05-02', 280, 100, 'Stranded copper wire measured in metres'),
    ('TRN-2N2222A', '2N2222A Transistor', 'Transistor', 'C-02-08', 125, 40, 'NPN switching transistor'),
    ('TRN-2N2907A', '2N2907A Transistor', 'Transistor', 'C-02-09', 38, 40, 'PNP switching transistor'),
    ('ADP-12V-5A', '12 V Power Adapter', 'Adapter', 'D-01-03', 9, 5, 'Regulated 60 W desktop adapter'),
    ('CON-JST-XH4', 'JST XH 4-pin Connector', 'Connector', 'C-04-11', 75, 25, 'Board connector with 2.5 mm pitch'),
    ('SNS-TEMP-01', 'Digital Temperature Sensor', 'Sensor', 'C-06-02', 14, 8, 'I2C temperature sensor module'),
    ('FUS-5A-FAST', '5 A Fast-Acting Fuse', 'Fuse', 'D-04-06', 60, 20, 'Replaceable cartridge fuse');
