package kr.jeommechurial.service;

import java.util.Optional;

public interface MenuPriceLookup {
    Optional<Price> find(String restaurantName, String restaurantAddress, String officeAddress, String winningMenu);

    record Price(String menu, int won, String source, String asOf) {}
}
