package com.itbaizhan.travelcommon.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CodeEnum {
    SUCCESS(200,"ok"),
    SYSTEM_ERROR(500,"系统异常,请重新尝试"), //系统异常
    PARAMETER_ERROR(601,"参数异常"),
    //添加商品类型异常
    INSERT_PRODUCT_TYPE_ERROR(602,"三级类型不能添加子类型"),
    //删除商品类型异常
    DELETE_PRODUCT_TYPE_ERROE(603,"删除/修改商品类型异常"),
    UPLOAD_FILE_ERROR(604,"文件上传异常"),
    REGISTER_CODE_ERROR(605,"验证码错误"),
    REGISTER_REPEAT_PHONE_ERROR(606,"手机号已存在"),
    REGISTER_REPEAT_NAME_ERROR(607,"用户名已存在"),
    LOGIN_NAME_PASSWORD_ERROR(608,"用户名或密码错误"),
    LOGIN_NO_PHONE_ERROR(610,"手机号没有注册"),
    LOGIN_USER_STATUS_ERROR(611,"用户状态异常"),
    QR_CODE_ERROR(612,"二维码生成异常"),
    CHECK_SIGN_ERROR(613,"支付宝验签异常"),
    ORDERS_STATUS_ERROR(614,"订单状态异常"),
    NO_STOCK_ERROR(615,"库存不足"),
    ORDERS_EXPIRED_ERROR(616,"订单不存在或超时"),
    MAIL_SEND_ERROR(617,"警告邮件发送失败"),
    FILE_DOWNLOAD_ERROR(619,"文件下载异常"),
    USER_ERROR(620,"用户失效，请重新登录"),

    AI_IMAGE_ERROR(621,"请上传图片文件"),
    AI_IMAGE_FORMAT_ERROR(622,"只支持图片文件"),
    AI_WEATHER_ERROR(623,"天气信息获取异常"),
    AI_WEATHER_SEARCH_ERROR(624,"天气预报查询失败"),
    AI_WEATHER_LOCATION_ERROR(625,"逆地理编码失败"),
    AI_CHAT_STATUS_ERROR(626,"此会话正在生成，请稍后重试"),
    AI_SESSION_IS_EXIST_ERROR(627,"会话不存在，请先创建会话"),
    CONTENT_VIOLATION(628,"输入包含违规内容，请修改后重试"),

    TRIP_NOT_FOUND(700,"行程未找到"),
    TRIP_GENERATE_ERROR(701,"行程生成异常"),
    TRIP_SCHEDULES_NOT_FOUND(702,"行程日程未找到"),
    TRIP_DIRECT_TRAIN_ERROR(703,"直达火车查询异常"),
    TRIP_HOTEL_ERROR(704,"酒店信息异常,请重新选择"),
    TRIP_TICKET_ERROR(705,"景点门票信息异常,请重新选择"),
    TRIP_TRANSPORT_ERROR(706,"交通信息异常,请重新选择"),
    TRIP_NO_FOUND_FILE(707,"行程文件未找到"),
    TRIP_TRANS_TYPE_ERROR(708,"请选择交通工具类型"),
    TRIP_FLIGHT_CODE_ERROR(709,"查询机场错误，请重新选择"),
    TRIP_CITY_CODE_ERROR(709,"查询城市错误，请重新选择"),
    TRIP_SEARCH_ERROR(710,"查询失败，请重新尝试"),
    TRIP_SEARCH_TYPE_ERROR(711,"查询失败，不能查询未存在的列"),
    TRIP_SAVE_ERROR(712,"形成保存失败，请重试"),
    TRIP_BACKUP_ERROR(713,"备份失败，请重新尝试"),
    TRIP_BACKUP_RESTORE_ERROR(714,"备份回复失败，请重新尝试"),
    TRIP_BACKUP_NOT_ERROR(714,"暂无备份"),
    TRIP_BACKUP_EXPIRE_ERROR(715,"备份已过期"),
    TRIP_STYLE_SIZE_ERROR(716,"风格数量不能超过10"),
    TRIP_DAY_SIZE_ERROR(717,"不能超过10天"),
    TRIP_GAODE_SIZE_ERROR(718,"住宿、景点、餐饮总和不能超过100"),
    TRIP_TRANSPORTATION_SIZE_ERROR(719,"交通不能超过20"),
    TRIP_ITEM_SIZE_ERROR(720,"每天计划数量不能超过10"),
    TRIP_AI_MODIFY_ERROR(721,"Ai修改失败，请重试"),
    TRIP_LOCKED(722,"行程正在删除/清空，请稍后再试"),
    TRIP_PROMPT_ERROR(723,"提示词不可用"),
    TRIP_STATUS_ERROR(724,"行程状态异常，请稍后重试"),
    TRIP_MODIFY_ERROR(725,"计划必须是草稿状态，才能修改"),
    TRIP_MD_ERROR(725,"计划没有最终计划文档，请生成文档再重试"),

    MANAGER_FAQ_SEARCH_ERROR(800,"问题和城市必须有一个"),
    MANAGER_USER_PASSWORD_ERROR(801,"密码修改失败")
    ;
    private final Integer code;
    private final String message;
}
