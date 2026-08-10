package com.nightbeam.donutshards.zone;

import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;import java.io.File;import java.io.IOException;import java.nio.file.Files;import java.nio.file.Path;import java.util.UUID;import static org.assertj.core.api.Assertions.*;

class ZoneServiceTest{
 @Test void tracksMembership(){var service=new ZoneService(null,new File("missing-zones.yml"));var id=UUID.randomUUID();assertThat(service.isInZone(id)).isFalse();service.enter(id,"glaze-ring");assertThat(service.current(id)).contains("glaze-ring");service.leave(id);assertThat(service.current(id)).isEmpty();}
 @Test void sphereContainsPoint(){var zone=new AfkZone("hub","world",0,64,0,10);assertThat(zone.contains("world",5,64,0)).isTrue();assertThat(zone.contains("world",11,64,0)).isFalse();assertThat(zone.contains("other",0,64,0)).isFalse();}
 @Test void loadsZonesFromYaml(@TempDir Path dir)throws IOException{var file=dir.resolve("zones.yml").toFile();Files.writeString(file.toPath(),"schema-version: 1\ndefault-radius: 10.0\nzones:\n  glaze-ring:\n    world: world\n    x: 100.0\n    y: 64.0\n    z: -50.0\n    radius: 12.0\n");var service=new ZoneService(null,file);assertThat(service.zone("glaze-ring")).isPresent();assertThat(service.zone("glaze-ring").get().radius()).isEqualTo(12.0);}
 @Test void deletePersists(@TempDir Path dir)throws IOException{var file=dir.resolve("zones.yml").toFile();Files.writeString(file.toPath(),"schema-version: 1\ndefault-radius: 10.0\nzones:\n  temp:\n    world: world\n    x: 0.0\n    y: 64.0\n    z: 0.0\n    radius: 5.0\n");var service=new ZoneService(null,file);assertThat(service.deleteZone("temp")).isTrue();service=new ZoneService(null,file);assertThat(service.zone("temp")).isEmpty();}
}
