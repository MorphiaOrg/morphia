package org.mongodb.morphia.issue155;


import org.mongodb.morphia.testutil.TestEntity;


class ContainerEntity extends TestEntity {
  private static final long serialVersionUID = 1L;

  final Bar foo = EnumBehindAnInterface.A;
}