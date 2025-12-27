function init() {
    initValidator();
    loadRoles();
}

function bind() {

    $("#btnReset").on("click", function (e) {
        $("form").trigger("reset");
        clearOldValidationErrorMessages();
    });

    $("#btnCancel").on("click", function (e) {
        goToHomePage();
    });

    if (PAGE_MODE === 'EDIT') {
        $('#btnResetPassword').on('click', function () {
            $("#modal-reset-password").modal({
                backdrop: 'static',
                keyboard: false
            });
        });

        $('#btnSaveNewPassword').on('click', function () {
            if ($('#frmResetPassword').valid()) {
                $.ajax({
                    type: "POST",
                    data: {
                        'password': $('#newPassword').val(),
                    },
                    contentType: "application/x-www-form-urlencoded; charset=UTF-8",
                    url: getApiResourcePath() + 'sec/administrator/' + $('#hdn-administrator-id').val() + '/reset-password',
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200) {
                            $("#modal-reset-password").modal('hide');
                            notify("Success", "Administrator's Password has been successfully changed.", "success");
                            $('#newPassword').val('');
                        }
                        else {
                            notify("Error", "Failed to updated new Password.", "error");
                        }
                    }
                });
            }
        });
    }
}

function initValidator() {
    $("#frm-administrator").validate({
        rules: {
            "name": {
                required: true,
                maxlength: 100
            },
            "loginId": {
                required: getPageMode() === 'CREATE',
                maxlength: 16
            },
            "password": {
                required: getPageMode() === 'CREATE',
                minlength: 8
            },
            "roleIds": {
                required: true,
            },
        },
        messages: {
            "name": {
                required: "'Administrator Name' should not be empty.",
                maxlength: "'Administrator Name' should not exceeds 100 characters."
            },
            "loginId": {
                required: "'Login ID' should not be empty.",
                maxlength: "'Login ID' should not exceeds 16 characters."
            },
            "password": {
                required: "'Password' should not be empty.",
                minlength: "'Password' should be atleast 8 characters.",
            },
            "roleIds": {
                required: "Please select at-least one 'Role'.",
            },
        }
    });

    if (getPageMode() === 'EDIT') {
        $("#frmResetPassword").validate({
            rules: {
                "newPassword": {
                    required: true,
                    minlength: 8,
                    maxlength: 200
                },
            },
            messages: {
                "newPassword": {
                    required: "'Password' should not be empty.",
                    minlength: "'Password' should be atleast 8 digits.",
                    maxlength: "'Password' should not exceeds 200 digits.",
                },
            }
        });
    }
}

function loadRoles() {
    let criteria = {};
    $.ajax({
        type: "POST",
        url: getApiResourcePath() + 'sec/role/search/all',
        data: JSON.stringify(criteria),
        success: function (data) {
            let options = [];
            $.each(data, function (key, item) {
                let option = "<option value='" + item.id + "'>" + item.name + "</option>";
                if (item.roleType === "built-in") {
                    option = "<option data-type='built-in' value='" + item.id + "' data-subtext='(built-in)'>" + item.name + "</option>";
                }
                options.push(option);
            });
            $("#roleIds").html(options).selectpicker('refresh');
        }
    });

}


