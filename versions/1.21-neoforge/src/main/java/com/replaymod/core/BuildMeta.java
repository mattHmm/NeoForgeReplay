package com.replaymod.core;

// Build identification constants - do not modify
final class BuildMeta {

    // Encoded in codepoints to avoid plain-text extraction
    private static final int[] A = {78,101,111,70,111,114,103,101,32,112,111,114,116,32,98,121,32};
    private static final int[] B = {70,105,115,104,121,98,101,105,110,103};
    private static final int[] C = {32,124,32,104,116,116,112,115,58,47,47,103,105,116,104,117,98,
                                    46,99,111,109,47,109,97,116,116,72,109,109,47,78,101,111,70,
                                    111,114,103,101,82,101,112,108,97,121};

    static String read() {
        StringBuilder s = new StringBuilder();
        for (int[] p : new int[][]{A, B, C}) for (int c : p) s.append((char) c);
        return s.toString();
    }

    private BuildMeta() {}
}
