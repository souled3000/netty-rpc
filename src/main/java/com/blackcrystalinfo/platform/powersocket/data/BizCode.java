package com.blackcrystalinfo.platform.powersocket.data;

/** 消息编码类型 */
public enum BizCode {
	/** 1.用户成功加入到家庭 */
	FamilyAddSuccess(1),
	/** 2.用户被傢庭住用戶刪除 */
	FamilyRemoveMember(2),
	/** 3.用户退出傢庭 */
	FamilyQuit(3),
	/** 4.用户被解散 */
	FamilyDismiss(4),
	/** 5.邀請用戶加入傢庭 */
	FamilyInvite(5),
	/** 6.用户拒絕加入傢庭 */
	FamilyRefuse(6),

	/** 7.設備綁定成功 */
	DeviceBindSuccess(7),
	/** 8.设备解绑成功 */
	DeviceUnBind(8),
	/** 9.设备上线 */
	DeviceOnLine(9),
	/** 10.设备下线 */
	DeviceOffLine(10),

	/** 11.用户邮件成功激活 */
	UserActivateSuccess(11),
	/** 12.用户手机号码绑定成功 */
	UserPhoneBindSuccess(12);

	private int value;

	private BizCode(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
