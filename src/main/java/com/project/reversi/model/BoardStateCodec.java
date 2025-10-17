package com.project.reversi.model;

import com.project.reversi.dto.BoardDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.util.List;

public final class BoardStateCodec {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final CollectionType LIST_OF_LISTS_TYPE = OBJECT_MAPPER.getTypeFactory()
      .constructCollectionType(List.class,
          OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));

  private BoardStateCodec() {
  }

  public static String encode(Board board) {
    if (board == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(BoardDTO.fromBoard(board).getBoardCells());
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize board state", e);
    }
  }

  public static Board decode(String payload) {
    if (payload == null || payload.isBlank()) {
      return null;
    }
    try {
      List<List<String>> cells = OBJECT_MAPPER.readValue(payload, LIST_OF_LISTS_TYPE);
      return Board.fromSnapshot(cells);
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize board state", e);
    }
  }
}
