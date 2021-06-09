package com.urovo;

import com.rfid.config.CMD;
import com.rfid.config.ERROR;

/**
 * @author naz
 * Email 961057759@qq.com
 * Date 2020/1/2
 */
public class FormatUtils {

    /**
     * 命令码和错误码解析
     *
     * @param cmd       命令码
     * @param errorCode 错误码
     * @return str
     */
    public static String format(byte cmd, byte errorCode) {
        if (errorCode == ERROR.SUCCESS) {
            return cmdFormat(cmd) + " succeeded";
        } else if (errorCode == ERROR.FAIL) {
            return cmdFormat(cmd) + " failed";
        }
        return cmdFormat(cmd) + " failed, reason for failure: " + errorFormat(errorCode);
    }

    /**
     * 命令码解析
     *
     * @param cmd 命令码
     * @return str
     */
    public static String cmdFormat(byte cmd) {
        String strCmd;
        switch (cmd) {
            case CMD.RESET:
                strCmd = "Reset reader";
                break;
            case CMD.SET_UART_BAUDRATE:
                strCmd = "Set baud rate of serial port";
                break;
            case CMD.GET_FIRMWARE_VERSION:
                strCmd = "Get firmware version";
                break;
            case CMD.SET_READER_ADDRESS:
                strCmd = "Set reader address";
                break;
            case CMD.SET_WORK_ANTENNA:
                strCmd = "Set working antenna";
                break;
            case CMD.GET_WORK_ANTENNA:
                strCmd = "Query current working antenna";
                break;
            case CMD.SET_OUTPUT_POWER:
                strCmd = "Set RF output power";
                break;
            case CMD.GET_OUTPUT_POWER:
                strCmd = "Query RF output power";
                break;
            case CMD.SET_FREQUENCY_REGION:
                strCmd = "Set RF frequency spectrum";
                break;
            case CMD.GET_FREQUENCY_REGION:
                strCmd = "Query RF frequency spectrum";
                break;
            case CMD.SET_BEEPER_MODE:
                strCmd = "Set reader buzzer behavior";
                break;
            case CMD.GET_READER_TEMPERATURE:
                strCmd = "Check reader internal temperature";
                break;
            case CMD.READ_GPIO_VALUE:
                strCmd = "Get GPIO1 GPIO2 status";
                break;
            case CMD.WRITE_GPIO_VALUE:
                strCmd = "Set GPIO3 GPIO4 status";
                break;
            case CMD.SET_ANT_CONNECTION_DETECTOR:
                strCmd = "Set antenna detector status";
                break;
            case CMD.GET_ANT_CONNECTION_DETECTOR:
                strCmd = "Get antenna detector status";
                break;
            case CMD.SET_TEMPORARY_OUTPUT_POWER:
                strCmd = "Set RF power without saving to flash";
                break;
            case CMD.SET_READER_IDENTIFIER:
                strCmd = "Set reader identification bytes";
                break;
            case CMD.GET_READER_IDENTIFIER:
                strCmd = "Get reader identification bytes";
                break;
            case CMD.SET_RF_LINK_PROFILE:
                strCmd = "Set RF link profile";
                break;
            case CMD.GET_RF_LINK_PROFILE:
                strCmd = "Get RF link profile";
                break;
            case CMD.GET_RF_PORT_RETURN_LOSS:
                strCmd = "Get current antenna port return loss";
                break;
            case CMD.INVENTORY:
                strCmd = "Inventory";
                break;
            case CMD.READ_TAG:
                strCmd = "Read EPC C1G2 tag(s)";
                break;
            case CMD.WRITE_TAG:
                strCmd = "Write EPC C1G2 tag(s)";
                break;
            case CMD.LOCK_TAG:
                strCmd = "Lock EPC C1G2 tag(s)";
                break;
            case CMD.KILL_TAG:
                strCmd = "Kill EPC C1G2 tag(s)";
                break;
            case CMD.SET_ACCESS_EPC_MATCH:
                strCmd = "Set tag access filter by EPC";
                break;
            case CMD.GET_ACCESS_EPC_MATCH:
                strCmd = "Get tag access filter by EPC";
                break;
            case CMD.REAL_TIME_INVENTORY:
                strCmd = "Inventory tags in real time mode";
                break;
            case CMD.FAST_SWITCH_ANT_INVENTORY:
                strCmd = "Real time inventory with fast ant switch";
                break;
            case CMD.CUSTOMIZED_SESSION_TARGET_INVENTORY:
                strCmd = "Inventory with desired session and inventoried flag";
                break;
            case CMD.SET_IMPINJ_FAST_TID:
                strCmd = "Set impinj FastTID function(Without saving to FLASH)";
                break;
            case CMD.SET_AND_SAVE_IMPINJ_FAST_TID:
                strCmd = "Set impinj FastTID function(Save to FLASH)";
                break;
            case CMD.GET_IMPINJ_FAST_TID:
                strCmd = "Get current FastTID setting";
                break;
            case CMD.ISO18000_6B_INVENTORY:
                strCmd = "Inventory 18000 6B tag(s)";
                break;
            case CMD.ISO18000_6B_READ_TAG:
                strCmd = "Read 18000 6B tag";
                break;
            case CMD.ISO18000_6B_WRITE_TAG:
                strCmd = "Write 18000-6B tag";
                break;
            case CMD.ISO18000_6B_LOCK_TAG:
                strCmd = "Lock 18000-6B tag data byte";
                break;
            case CMD.ISO18000_6B_QUERY_LOCK_TAG:
                strCmd = "Query lock 18000-6B tag data byte";
                break;
            case CMD.GET_INVENTORY_BUFFER:
                strCmd = "Get buffered data without clearing";
                break;
            case CMD.GET_AND_RESET_INVENTORY_BUFFER:
                strCmd = "Get and clear buffered data";
                break;
            case CMD.GET_INVENTORY_BUFFER_TAG_COUNT:
                strCmd = "Query how many tags are buffered";
                break;
            case CMD.RESET_INVENTORY_BUFFER:
                strCmd = "Clear buffer";
                break;
            case CMD.QUERY_READER_STATUS:
                strCmd = "Query reader status";
                break;
            case CMD.SET_READER_STATUS:
                strCmd = "Set reader model";
                break;
            default:
                strCmd = "Unknown operate";
                break;
        }
        return strCmd;
    }

    /**
     * 错误码解析
     *
     * @param errorCode 错误码
     * @return str
     */
    private static String errorFormat(byte errorCode) {
        String strErrorCode;
        switch (errorCode) {
//            case ERROR.SUCCESS:
//                strErrorCode = UHFApplication.getContext().getResources()
//                        .getString(R.string.command_succeeded);
//                break;
//            case ERROR.FAIL:
//                strErrorCode = UHFApplication.getContext().getResources()
//                        .getString(R.string.command_failed);
//                break;
            case ERROR.MCU_RESET_ERROR:
                strErrorCode = "CUP reset error";
                break;
            case ERROR.CW_ON_ERROR:
                strErrorCode = "Turn on CW error";
                break;
            case ERROR.ANTENNA_MISSING_ERROR:
                strErrorCode = "Antenna is missing";
                break;
            case ERROR.WRITE_FLASH_ERROR:
                strErrorCode = "Write flash error";
                break;
            case ERROR.READ_FLASH_ERROR:
                strErrorCode = "Read flash error";
                break;
            case ERROR.SET_OUTPUT_POWER_ERROR:
                strErrorCode = "Set output power error";
                break;
            case ERROR.TAG_INVENTORY_ERROR:
                strErrorCode = "Error occurred when inventory";
                break;
            case ERROR.TAG_READ_ERROR:
                strErrorCode = "Error occurred when read";
                break;
            case ERROR.TAG_WRITE_ERROR:
                strErrorCode = "Error occurred when write";
                break;
            case ERROR.TAG_LOCK_ERROR:
                strErrorCode = "Error occurred when lock";
                break;
            case ERROR.TAG_KILL_ERROR:
                strErrorCode = "Error occurred when kill";
                break;
            case ERROR.NO_TAG_ERROR:
                strErrorCode = "There is no tag to be operated";
                break;
            case ERROR.INVENTORY_OK_BUT_ACCESS_FAIL:
                strErrorCode = "Tag Inventoried but access failed";
                break;
            case ERROR.BUFFER_IS_EMPTY_ERROR:
                strErrorCode = "Buffer is empty";
                break;
            case ERROR.ACCESS_OR_PASSWORD_ERROR:
                strErrorCode = "Access failed or wrong password";
                break;
            case ERROR.PARAMETER_INVALID:
                strErrorCode = "Invalid parameter";
                break;
            case ERROR.PARAMETER_INVALID_WORDCNT_TOO_LONG:
                strErrorCode = "WordCnt is too long";
                break;
            case ERROR.PARAMETER_INVALID_MEMBANK_OUT_OF_RANGE:
                strErrorCode = "MemBank out of range";
                break;
            case ERROR.PARAMETER_INVALID_LOCK_REGION_OUT_OF_RANGE:
                strErrorCode = "Lock region out of range";
                break;
            case ERROR.PARAMETER_INVALID_LOCK_ACTION_OUT_OF_RANGE:
                strErrorCode = "LockType out of range";
                break;
            case ERROR.PARAMETER_READER_ADDRESS_INVALID:
                strErrorCode = "Invalid reader address";
                break;
            case ERROR.PARAMETER_INVALID_ANTENNA_ID_OUT_OF_RANGE:
                strErrorCode = "AntennaID out of range";
                break;
            case ERROR.PARAMETER_INVALID_OUTPUT_POWER_OUT_OF_RANGE:
                strErrorCode = "Output power out of range";
                break;
            case ERROR.PARAMETER_INVALID_FREQUENCY_REGION_OUT_OF_RANGE:
                strErrorCode = "Frequency region out of range";
                break;
            case ERROR.PARAMETER_INVALID_BAUDRATE_OUT_OF_RANGE:
                strErrorCode = "Baud rate out of range";
                break;
            case ERROR.PARAMETER_BEEPER_MODE_OUT_OF_RANGE:
                strErrorCode = "Buzzer behavior out of range";
                break;
            case ERROR.PARAMETER_EPC_MATCH_LEN_TOO_LONG:
                strErrorCode = "EPC match is too long";
                break;
            case ERROR.PARAMETER_EPC_MATCH_LEN_ERROR:
                strErrorCode = "EPC match length wrong";
                break;
            case ERROR.PARAMETER_INVALID_EPC_MATCH_MODE:
                strErrorCode = "Invalid EPC match mode";
                break;
            case ERROR.PARAMETER_INVALID_FREQUENCY_RANGE:
                strErrorCode = "Invalid frequency range";
                break;
            case ERROR.FAIL_TO_GET_RN16_FROM_TAG:
                strErrorCode = "Failed to receive RN16 from tag";
                break;
            case ERROR.PARAMETER_INVALID_DRM_MODE:
                strErrorCode = "Invalid DRM mode";
                break;
            case ERROR.PLL_LOCK_FAIL:
                strErrorCode = "PLL can not lock";
                break;
            case ERROR.RF_CHIP_FAIL_TO_RESPONSE:
                strErrorCode = "No response from RF chip";
                break;
            case ERROR.FAIL_TO_ACHIEVE_DESIRED_OUTPUT_POWER:
                strErrorCode = "Can not achieve desired output power level";
                break;
            case ERROR.COPYRIGHT_AUTHENTICATION_FAIL:
                strErrorCode = "Can not authenticate firmware copyright";
                break;
            case ERROR.SPECTRUM_REGULATION_ERROR:
                strErrorCode = "Spectrum regulation wrong";
                break;
            case ERROR.OUTPUT_POWER_TOO_LOW:
                strErrorCode = "Output power too low";
                break;
            default:
                strErrorCode = "Unknown Error";
                break;
        }
        return strErrorCode;
    }
}
