package com.soundmap.music.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 곡 정보 캐시.
 *
 * <p>외부 API(iTunes)에서 받은 결과를 우리 DB 에 한 번 저장해두고 재사용한다.
 * 같은 곡을 여러 사람이 들어도 행은 하나만 생긴다.
 */
@Entity
@Table(
        name = "tracks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tracks_source_external_id",
                columnNames = {"source", "external_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackSource source;

    /** 외부 서비스에서의 곡 식별자 (iTunes 의 trackId). */
    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 200)
    private String artistName;

    @Column(name = "album_name", length = 300)
    private String albumName;

    @Column(name = "album_art_url", length = 500)
    private String albumArtUrl;

    /**
     * 30초 미리듣기 주소. iTunes 가 검색 결과에 함께 준다.
     *
     * <p>곡에 따라 없을 수 있어서 nullable 이다. 이 값이 비어 있으면
     * 화면에서 재생 버튼을 감춘다.
     */
    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Builder
    private Track(TrackSource source, String externalId, String title,
                  String artistName, String albumName, String albumArtUrl,
                  String previewUrl) {
        this.source = source;
        this.externalId = externalId;
        this.title = title;
        this.artistName = artistName;
        this.albumName = albumName;
        this.albumArtUrl = albumArtUrl;
        this.previewUrl = previewUrl;
    }

    /**
     * 미리듣기 주소가 비어 있을 때만 채운다.
     *
     * <p>미리듣기 기능이 생기기 전에 저장된 곡은 이 값이 없다. 같은 곡을 다시
     * 고르면 검색 결과에 주소가 들어 있으므로 그때 메워 준다. 이미 값이 있으면
     * 건드리지 않아서, 곡을 고를 때마다 불필요한 UPDATE 가 나가지 않는다.
     */
    public void fillPreviewUrlIfAbsent(String previewUrl) {
        if (this.previewUrl == null && previewUrl != null && !previewUrl.isBlank()) {
            this.previewUrl = previewUrl;
        }
    }
}
