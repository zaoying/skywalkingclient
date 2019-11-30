package qo;

import lombok.Data;

import java.util.List;

@Data
//@ApiModel
public class BaseDurationQo {

//    @NotNull(message = "开始时间[start]不能为空")
//    @ApiModelProperty(value = "开始时间")
    String start;

//    @NotNull(message = "结束时间[end]不能为空")
//    @ApiModelProperty(value = "结束时间")
    String end;

//    @NotNull(message = "时间单位[step]不能为空")
//    @ApiModelProperty(value = "时间单位")
    String step;
}
