package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mongodb.client.model.TimeSeriesGranularity;

import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.mapping.Mapper;

import static com.mongodb.client.model.TimeSeriesGranularity.SECONDS;

/**
 * Defines options for creating time series collections.
 *
 * @morphia.experimental
 * @since 2.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE })
@MorphiaExperimental
@interface TimeSeriesOptions {
    TimeSeriesGranularity granularity() default SECONDS;

    String metaField() default Mapper.IGNORED_FIELDNAME;

    String timeField() default Mapper.IGNORED_FIELDNAME;
}
