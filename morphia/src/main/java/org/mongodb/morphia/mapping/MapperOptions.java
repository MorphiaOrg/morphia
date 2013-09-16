package org.mongodb.morphia.mapping;


import org.mongodb.morphia.ObjectFactory;


/**
 * Options to control mapping behavior.
 *
 * @author Scott Hernandez
 */
public class MapperOptions {
  /**
   * <p>Treat java transient fields as if they have {@code @Transient} on them</p>
   */
  public boolean actLikeSerializer;
  /**
   * <p>Controls if null are stored. </p>
   */
  public boolean storeNulls;
  /**
   * <p>Controls if empty collection/arrays are stored. </p>
   */
  public boolean storeEmpties;
  /**
   * <p>Controls if final fields are stored. </p>
   */
  public boolean ignoreFinals; //ignore final fields.

  public final CustomMapper referenceMapper = new ReferenceMapper();
  public final CustomMapper embeddedMapper  = new EmbeddedMapper();
  public final CustomMapper valueMapper     = new ValueMapper();
  public final CustomMapper defaultMapper   = embeddedMapper;

  public ObjectFactory objectFactory = new DefaultCreator();
}
