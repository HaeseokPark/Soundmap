package com.soundmap.music.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="saved_tracks", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","track_id"}))
public class SavedTrack {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false) private Long userId;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="track_id") private Track track;
    @Column(nullable=false) private LocalDateTime createdAt;
    public SavedTrack(Long userId, Track track) { this.userId=userId; this.track=track; createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")); }
}
