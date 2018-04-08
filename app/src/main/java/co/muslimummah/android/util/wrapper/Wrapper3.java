package co.muslimummah.android.util.wrapper;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Xingbo.Jie on 4/6/17.
 */
@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class Wrapper3<E1, E2, E3> implements Serializable {
    public E1 entity1;
    public E2 entity2;
    public E3 entity3;

}
