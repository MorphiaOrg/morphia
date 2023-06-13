package dev.morphia.test.models.errors.twoIds;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
public class TwoIds {
    @Id
    private String extraId;
    @Id
    private String broken;
}
