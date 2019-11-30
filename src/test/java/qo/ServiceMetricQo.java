package qo;

import lombok.Data;

import java.util.List;

@Data
public class ServiceMetricQo {
//    @NotNull(message = "服务名[names]不能为空")
    List<String> names;
    BaseDurationQo duration;
}