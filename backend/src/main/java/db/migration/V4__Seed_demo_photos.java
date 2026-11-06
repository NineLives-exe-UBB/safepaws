package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Loads the demo photos from resources/db/demo-photos (sources are listed in SOURCES.md there).
 * Written in Java because the image files are too large to embed in an SQL migration.
 */
public class V4__Seed_demo_photos extends BaseJavaMigration {

    private static final String PHOTO_FOLDER = "/db/demo-photos/";

    /**
     * Announcements are matched by their placeholder phone. An announcement's first photo becomes
     * its main one, unless it already has a main photo.
     */
    private static final String INSERT_PHOTO = """
            INSERT INTO photo (announcement_id, data, content_type, file_size, original_file_name, is_main)
            SELECT a.id, ?, 'image/jpeg', ?, ?,
                   NOT EXISTS (SELECT 1 FROM photo p WHERE p.announcement_id = a.id AND p.is_main)
            FROM announcement a
            WHERE a.contact_phone = ?
            """;

    private record DemoPhoto(String contactPhone, String fileName) {
    }

    private static final List<DemoPhoto> PHOTOS = List.of(
            new DemoPhoto("0700000003", "orange-tabby.jpg"),
            new DemoPhoto("0700000004", "mixed-dog.jpg"),
            new DemoPhoto("0700000005", "kiwi-budgerigar.jpg"),
            new DemoPhoto("0700000006", "luna-siamese.jpg"),
            new DemoPhoto("0700000007", "labrador.jpg"),
            new DemoPhoto("0700000008", "bobi.jpg"),
            new DemoPhoto("0700000009", "kitten-1.jpg"),
            new DemoPhoto("0700000009", "kitten-2.jpg"),
            new DemoPhoto("0700000010", "bunny.jpg"),
            new DemoPhoto("0700000011", "tom-british.jpg"),
            new DemoPhoto("0700000012", "husky.jpg"),
            new DemoPhoto("0700000013", "max-beagle-1.jpg"),
            new DemoPhoto("0700000013", "max-beagle-2.jpg"),
            new DemoPhoto("0700000014", "pigeon.jpg"),
            new DemoPhoto("0700000015", "pufi.jpg"),
            new DemoPhoto("0700000016", "schnauzer.jpg"),
            new DemoPhoto("0700000017", "rocky.jpg"),
            new DemoPhoto("0700000018", "tuxedo.jpg"),
            new DemoPhoto("0700000019", "coco.jpg"),
            new DemoPhoto("0700000020", "rabbit.jpg"),
            new DemoPhoto("0700000021", "mia-1.jpg"),
            new DemoPhoto("0700000021", "mia-2.jpg"),
            new DemoPhoto("0700000022", "jack-russell.jpg"));

    @Override
    public void migrate(Context context) throws Exception {
        try (PreparedStatement statement = context.getConnection().prepareStatement(INSERT_PHOTO)) {
            for (DemoPhoto photo : PHOTOS) {
                byte[] data = readPhoto(photo.fileName());
                statement.setBytes(1, data);
                statement.setLong(2, data.length);
                statement.setString(3, photo.fileName());
                statement.setString(4, photo.contactPhone());
                statement.executeUpdate();
            }
        }
    }

    private static byte[] readPhoto(String fileName) throws IOException {
        try (InputStream stream = V4__Seed_demo_photos.class.getResourceAsStream(PHOTO_FOLDER + fileName)) {
            if (stream == null) {
                throw new IllegalStateException("Demo photo not found: " + fileName);
            }
            return stream.readAllBytes();
        }
    }
}
