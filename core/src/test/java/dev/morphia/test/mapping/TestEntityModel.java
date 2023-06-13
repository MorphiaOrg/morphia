package dev.morphia.test.mapping;

import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.generics.Another;
import dev.morphia.test.models.generics.Child;
import dev.morphia.test.models.generics.EmbeddedType;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.util.List.of;
import static org.testng.Assert.assertEquals;

@SuppressWarnings("DataFlowIssue")
public class TestEntityModel extends TestBase {
    @Test
    public void childParentPairings() {
        withConfig(buildConfig()
                .mapPackages(of(ChildLevel3a.class.getPackageName())), () -> {
                    EntityModel entityModel = getMapper().getEntityModel(RootParent.class);
                    Set<EntityModel> subtypes = entityModel.getSubtypes();
                    assertEquals(subtypes.size(), 6);
                    checkParent(RootParent.class, ChildLevel1a.class, ChildLevel1b.class, ChildLevel1c.class);
                    checkParent(ChildLevel1a.class, ChildLevel2a.class, ChildLevel2b.class);
                    checkParent(ChildLevel2b.class, ChildLevel3a.class);
                });

    }

    private void checkParent(Class<?> parent, Class<?>... classes) {
        for (var klass : classes) {
            assertEquals(getMapper().getEntityModel(klass).getSuperClass().getType(), parent);
        }
    }

    @Test
    public void subTypes() {
        getMapper().map(EmbeddedType.class, Another.class, Child.class);

        Mapper mapper = getMapper();
        Set<EntityModel> subTypes = mapper.getEntityModel(EmbeddedType.class).getSubtypes();
        Assert.assertTrue(subTypes.contains(mapper.getEntityModel(Another.class)));
        Assert.assertTrue(subTypes.contains(mapper.getEntityModel(Child.class)));
    }

}

@Entity
interface TestEntity {
}

class RootParent implements TestEntity {
    @Id
    ObjectId id = null;
}

@Entity
class ChildLevel1a extends RootParent {
}

class ChildLevel2a extends ChildLevel1a {
}

class ChildLevel2b extends ChildLevel1a {
}

class ChildLevel3a extends ChildLevel2b {
}

class ChildLevel1b extends RootParent {
}

class ChildLevel1c extends RootParent {
}
