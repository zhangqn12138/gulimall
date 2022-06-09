package com.atguigu.common.to;

import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @author QingnanZhang
 * @creat 2022-04-05 18:22
 **/
@Data
public class SpuBoundsTo {
    private Long spuId;
    private BigDecimal buyBounds;
    private BigDecimal growBounds;
}
