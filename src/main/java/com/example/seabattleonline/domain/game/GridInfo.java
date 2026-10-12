package com.example.seabattleonline.domain.game;

import domain.game.board.CellView;

import java.util.List;

public record GridInfo(List<CellView> player1Grid, List<CellView> player2Grid) {
}
