package org.wikimedia.lzma;

import java.util.EnumSet;
import java.util.Map;

import com.google.common.collect.Maps;

/** lzma_action analog (see: /usr/include/lzma/base.h) */
public enum Action {
    RUN(0), SYNC_FLUSH(1), FULL_FLUSH(2), FINISH(3);

    private static final Map<Integer, Action> index = Maps.newHashMap();

    static {
        for (Action e : EnumSet.allOf(Action.class)) {
            index.put(e.getCode(), e);
        }
    }

    private final int code;

    private Action(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public static Action fromCode(int code) {
        return index.get(code);
    }
}