function init() {
    initValidator();
    loadRoles();
    if ($('.branch-selectpicker').length > 0) {
        $('.branch-selectpicker').prop('disabled', true).selectpicker('refresh');
        initBanks();
    }
}

function bind() {

    $("#btnReset").on("click", function (e) {
        $("form").trigger("reset");
        clearOldValidationErrorMessages();
    });

    $("#btnCancel").on("click", function (e) {
        goToHomePage();
    });

    $(".bank-selectpicker").on("change", function (e) {
        if ($(this).val().length > 0) {
            loadBranches($(this).find("option:selected").data("orgid"));
        }
        else {
            $(".branch-selectpicker").prop('disabled', true).selectpicker('val', '').selectpicker('refresh');
        }
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
                    url: getApiResourcePath() + 'sec/staffs/' + $('#hdn-user-id').val() + '/reset-password',
                    success: function (response, textStatus, xhr) {
                        if (xhr.status === 200) {
                            $("#modal-reset-password").modal('hide');
                            notify("Success", "Staff's Password has been successfully changed.", "success");
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
    $("#frm-user").validate({
        rules: {
            "officerName": {
                required: true,
                maxlength: 100
            },
            "officerRank": {
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
            "contactPhone": {
                maxlength: 12
            },
            "roleIds": {
                required: true,
            },
        },
        messages: {
            "officerName": {
                required: "'Officer Name' should not be empty.",
                maxlength: "'Officer Name' should not exceeds 100 characters."
            },
            "officerRank": {
                required: "'Officer Rank' should not be empty.",
                maxlength: "'Officer Rank' should not exceeds 100 characters."
            },
            "loginId": {
                required: "'Login ID' should not be empty.",
                maxlength: "'Login ID' should not exceeds 16 characters."
            },
            "password": {
                required: "'Password' should not be empty.",
                minlength: "'Password' should be atleast 8 characters.",
            },
            "contactPhone": {
                maxlength: "'Phone' should not exceeds 12 characters."
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
    var criteria = {};
    $.ajax({
        type: "POST",
        url: getApiResourcePath() + 'sec/roles/search/list',
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

function initBanks() {
    let targetElems = ".bank-selectpicker";
    let criteria = {};
    // criteria.stateRegionCode = "MMR222";
    $.ajax({
        type: "GET",
        url: getApiResourcePath() + 'sec/master-data/general/banks',
        data: criteria,
        success: function (response, textStatus, xhr) {
            if (xhr.status === 200 && response) {
                let options = [];
                options.push('<option value="" selected="selected">Select Bank</option>');
                $.each(response, function (key, item) {
                    let option = "<option data-orgid = '" + item.id + "' value='" + item.id + "'>" + item.name + "</option>";
                    options.push(option);
                });
                $(targetElems).html(options).selectpicker('refresh');
            }
            else {
                notify("Error", "Failed to fetch bank informations.", "error");
            }
        }
    });
}

function loadBranches(organizationId) {
    if (isNotEmpty(organizationId)) {
        let targetElems = $('.branch-selectpicker');
        let criteria = {};
        criteria.organizationId = organizationId;
        $.ajax({
            type: "GET",
            url: getApiResourcePath() + 'sec/master-data/general/branches',
            data: criteria,
            success: function (response, textStatus, xhr) {
                if (xhr.status === 200 && response) {
                    let options = [];
                    options.push('<option value="" selected="selected">Select Branch</option>');
                    $.each(response, function (key, item) {
                        let option = "<option value='" + item.id + "'>" + item.name + "</option>";
                        options.push(option);
                    });
                    targetElems.html(options).selectpicker('refresh');
                    targetElems.prop("disabled", false).selectpicker('refresh');
                }
                else {
                    notify("Error", "Failed to fetch branch informations.", "error");
                }
            }
        });
    }
}

