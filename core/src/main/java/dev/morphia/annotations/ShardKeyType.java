package dev.morphia.annotations;

import dev.morphia.annotations.internal.MorphiaInternal;

public enum ShardKeyType {
    HASHED {
        @Override
        public Object queryForm() {
            return "hashed";
        }
    },
    RANGED {
        @Override
        public Object queryForm() {
            return 1;
        }
    };

    @MorphiaInternal
    public abstract Object queryForm();
}
