package com.soundmap.taste.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 취향 저장 요청. 기존 선택을 이 목록으로 통째로 교체한다.
 *
 * <p>가수를 최소 3명 받는 것은 6주차 취향 일치도 때문이다. 한두 명만
 * 골라두면 교집합이 0 아니면 100 으로 튀어서 숫자가 의미를 잃는다.
 */
public record TasteRequest(

        @NotNull(message = "좋아하는 가수를 선택해 주세요.")
        @Size(min = 3, max = 30, message = "좋아하는 가수는 3명 이상 30명 이하로 선택해 주세요.")
        List<Long> artistIds,

        @NotNull(message = "선호 장르를 선택해 주세요.")
        @Size(min = 1, max = 10, message = "선호 장르는 1개 이상 10개 이하로 선택해 주세요.")
        List<Long> genreIds
) {
}
