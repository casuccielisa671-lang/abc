package com.occupation.recommend.config;

import com.occupation.recommend.mapper.SysStudentProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 启动时对账清理孤儿证件照文件。
 * <p>
 * 证件照文件存磁盘 {@code data/avatars/}，而 {@code docker-compose down -v} 只清数据库、
 * 不动磁盘文件——重置后 {@code sys_student_profile.avatar_url} 归零，但旧 png 还在磁盘上，
 * 没有任何记录引用它们，成为永久孤儿、越堆越多。
 * <p>
 * 本清理器在应用就绪后扫描存储目录，删除“数据库里没有任何画像引用”的文件，
 * 使磁盘与数据库对齐（down -v 后种子无头像 → 旧文件全清；正常运行也顺带清换头像残留）。
 * 只删无引用文件，被引用的保留，安全。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarStartupCleaner {

    private static final String URL_PREFIX = "/api/avatars/";

    @Value("${app.avatar.storage-path:./data/avatars}")
    private String avatarStoragePath;

    private final SysStudentProfileMapper profileMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanOrphanAvatars() {
        try {
            Path root = Paths.get(avatarStoragePath);
            if (!Files.isDirectory(root)) {
                return;   // 还没上传过任何头像
            }

            // 数据库里被引用的文件（跨租户）→ 绝对规范化路径集合
            Set<Path> referenced = new HashSet<>();
            List<String> urls = profileMapper.selectAllAvatarUrls();
            for (String url : urls) {
                if (url != null && url.startsWith(URL_PREFIX)) {
                    referenced.add(root.resolve(url.substring(URL_PREFIX.length()))
                            .toAbsolutePath().normalize());
                }
            }

            // 扫描磁盘文件，删掉没被引用的
            int deleted = 0;
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> files = walk.filter(Files::isRegularFile).toList();
                for (Path f : files) {
                    if (!referenced.contains(f.toAbsolutePath().normalize())) {
                        try {
                            Files.delete(f);
                            deleted++;
                        } catch (Exception e) {
                            log.warn("删除孤儿证件照失败(忽略): {}", f, e);
                        }
                    }
                }
            }
            log.info("证件照对账清理完成：被引用 {} 张，删除孤儿文件 {} 个", referenced.size(), deleted);
        } catch (Exception e) {
            log.warn("证件照孤儿清理跳过（异常忽略，不影响启动）", e);
        }
    }
}
