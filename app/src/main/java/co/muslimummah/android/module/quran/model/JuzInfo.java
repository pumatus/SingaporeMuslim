package co.muslimummah.android.module.quran.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by frank on 8/24/17.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuzInfo implements Serializable {
    private static final long serialVersionUID = 3238416747125369261L;
    String juzOriginal;
    String juzEnglish;
}
