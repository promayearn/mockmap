package com.example.pro.mockmap.Modules;

import java.util.List;

/**
 * Created by Pro on 3/15/2017.
 */

public interface DirectionFinderListener {
    void onDirectionFinderStart();

    void onDirectionFinderSuccess(List<Route> route);
}
