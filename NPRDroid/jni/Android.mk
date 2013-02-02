LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := npr
LOCAL_SRC_FILES := npr.c

include $(BUILD_SHARED_LIBRARY)