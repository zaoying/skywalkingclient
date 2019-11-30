package qo;

import lombok.Data;

@Data
//@ApiModel
public class BaseNameSpaceQo {

//    @ApiModelProperty(value = "应用名称", required = false)
    private String project;

    //@NotNull(message = "应用标识[app]不能为空")
//    @ApiModelProperty(value = "应用标识", required = false)
    private String app;

//    @NotNull(message = "租户标识[env]不能为空")
//    @ApiModelProperty(value = "租户标识")
    private String env;

//    @ApiModelProperty(value = "分组", required = false)
    private String group;

//    @NotNull(message = "租户id不能为空")
//    @ApiModelProperty(value = "租户ID")
    private Long tenantId;
}
