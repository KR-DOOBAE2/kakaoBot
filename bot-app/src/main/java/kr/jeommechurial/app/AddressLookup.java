package kr.jeommechurial.app;

import java.util.List;

public interface AddressLookup {
    List<AddressResult> searchAddresses(String query);

    record AddressResult(String address, double latitude, double longitude) {}
}
