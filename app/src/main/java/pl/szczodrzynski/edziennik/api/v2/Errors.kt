/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2

/*const val CODE_OTHER                                        = 0
const val CODE_OK                                           = 1
const val CODE_NO_INTERNET                                  = 10
const val CODE_SSL_ERROR                                    = 13
const val CODE_ARCHIVED                                     = 5
const val CODE_MAINTENANCE                                  = 6
const val CODE_LOGIN_ERROR                                  = 7
const val CODE_ACCOUNT_MISMATCH                             = 8
const val CODE_APP_SERVER_ERROR                             = 9
const val CODE_MULTIACCOUNT_SETUP                           = 12
const val CODE_TIMEOUT                                      = 11
const val CODE_PROFILE_NOT_FOUND                            = 14
const val CODE_ATTACHMENT_NOT_AVAILABLE                     = 28
const val CODE_INVALID_LOGIN                                = 2
const val CODE_INVALID_SERVER_ADDRESS                       = 21
const val CODE_INVALID_SCHOOL_NAME                          = 22
const val CODE_INVALID_DEVICE                               = 23
const val CODE_OLD_PASSWORD                                 = 4
const val CODE_INVALID_TOKEN                                = 24
const val CODE_EXPIRED_TOKEN                                = 27
const val CODE_INVALID_SYMBOL                               = 25
const val CODE_INVALID_PIN                                  = 26
const val CODE_LIBRUS_NOT_ACTIVATED                         = 29
const val CODE_SYNERGIA_NOT_ACTIVATED                       = 32
const val CODE_LIBRUS_DISCONNECTED                          = 31
const val CODE_PROFILE_ARCHIVED                             = 30*/

const val ERROR_REQUEST_FAILURE                             = 50
const val ERROR_REQUEST_HTTP_400                            = 51
const val ERROR_REQUEST_HTTP_401                            = 52
const val ERROR_REQUEST_HTTP_403                            = 53
const val ERROR_REQUEST_HTTP_404                            = 54
const val ERROR_REQUEST_HTTP_405                            = 55
const val ERROR_REQUEST_HTTP_410                            = 56
const val ERROR_REQUEST_HTTP_500                            = 57
const val ERROR_RESPONSE_EMPTY                              = 100
const val ERROR_LOGIN_DATA_MISSING                          = 101
const val ERROR_LOGIN_DATA_INVALID                          = 102
const val ERROR_PROFILE_MISSING                             = 105
const val ERROR_INVALID_LOGIN_MODE                          = 110
const val ERROR_LOGIN_METHOD_NOT_SATISFIED                  = 111

const val CODE_INTERNAL_LIBRUS_ACCOUNT_410                  = 120
const val CODE_INTERNAL_LIBRUS_SYNERGIA_EXPIRED             = 121
const val ERROR_LOGIN_LIBRUS_API_CAPTCHA_NEEDED             = 124
const val ERROR_LOGIN_LIBRUS_API_CONNECTION_PROBLEMS        = 125
const val ERROR_LOGIN_LIBRUS_API_INVALID_CLIENT             = 126
const val ERROR_LOGIN_LIBRUS_API_REG_ACCEPT_NEEDED          = 127
const val ERROR_LOGIN_LIBRUS_API_CHANGE_PASSWORD_ERROR      = 128
const val ERROR_LOGIN_LIBRUS_API_PASSWORD_CHANGE_REQUIRED   = 129
const val ERROR_LOGIN_LIBRUS_API_INVALID_GRANT              = 130
const val ERROR_LOGIN_LIBRUS_API_OTHER                      = 131
const val ERROR_LOGIN_LIBRUS_PORTAL_CSRF_MISSING            = 132
const val ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED           = 133
const val ERROR_LOGIN_LIBRUS_PORTAL_ACTION_ERROR            = 134
const val ERROR_LOGIN_LIBRUS_PORTAL_TOKEN_ERROR             = 135
const val ERROR_LOGIN_LIBRUS_PORTAL_SYNERGIA_DISCONNECTED   = 136
const val ERROR_LOGIN_LIBRUS_PORTAL_SYNERGIA_410            = 137
const val ERROR_LOGIN_LIBRUS_PORTAL_SYNERGIA_NOT_FOUND      = 138
const val ERROR_LOGIN_LIBRUS_PORTAL_SYNERGIA_OTHER          = 139
const val ERROR_LOGIN_LIBRUS_PORTAL_SYNERGIA_TOKEN_MISSING  = 139
const val ERROR_LIBRUS_API_TOKEN_EXPIRED                    = 140
const val ERROR_LIBRUS_API_INSUFFICIENT_SCOPES              = 141
const val ERROR_LIBRUS_API_OTHER                            = 142
const val ERROR_LIBRUS_API_REQUEST_DENIED                   = 143
const val ERROR_LIBRUS_API_RESOURCE_NOT_FOUND               = 144
const val ERROR_LIBRUS_API_DATA_NOT_FOUND                   = 145
const val ERROR_LIBRUS_API_TIMETABLE_NOT_PUBLIC             = 146
const val ERROR_LIBRUS_API_RESOURCE_ACCESS_DENIED           = 147
const val ERROR_LIBRUS_API_INVALID_REQUEST_PARAMS           = 148
const val ERROR_LIBRUS_API_INCORRECT_ENDPOINT               = 149
const val ERROR_LIBRUS_API_LUCKY_NUMBER_NOT_ACTIVE          = 150
const val ERROR_LIBRUS_API_NOTES_NOT_ACTIVE                 = 151
const val ERROR_LOGIN_LIBRUS_SYNERGIA_NO_TOKEN              = 152
const val ERROR_LOGIN_LIBRUS_SYNERGIA_TOKEN_INVALID         = 153
const val ERROR_LOGIN_LIBRUS_SYNERGIA_NO_SESSION_ID         = 154

const val EXCEPTION_LOGIN_LIBRUS_API_TOKEN                  = 901
const val EXCEPTION_LOGIN_LIBRUS_PORTAL_TOKEN               = 902
const val EXCEPTION_LOGIN_LIBRUS_PORTAL_SYNERGIA_TOKEN      = 903
const val EXCEPTION_LIBRUS_API_REQUEST                      = 904