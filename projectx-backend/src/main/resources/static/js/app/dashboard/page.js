let targetMonth;

function init() {
    initAnalysis()
    targetMonth = $("#targetMonth")
    targetMonth
        .daterangepicker(
            {
                timePicker: false,
                timePickerSeconds: false,
                showDropdowns: true,
                autoApply: true,
                autoUpdateInput: false,
            });
    targetMonth.on('apply.daterangepicker', function (ev, picker) {
        $(this).val(picker.startDate.format('DD/MM/YYYY') + ' - ' + picker.endDate.format('DD/MM/YYYY'));

        let searchDto = {params: {}};
        searchDto.transactionDateFrom = picker.startDate.format("DD-MM-yyyy HH:mm:ss");
        searchDto.transactionDateTo = picker.endDate.format("DD-MM-yyyy HH:mm:ss");
        searchDto.singleTypes = [0, 1, 2, 3, 4];
        callAnalysis(searchDto);
    });
}

function bind() {
    $("#btnReset").on("click", function (e) {
        targetMonth.val("All");
        initAnalysis()
    });
}

function initAnalysis() {
    let searchDto = {params: {}}
    searchDto.singleTypes = [0, 1, 2, 3, 4];
    callAnalysis(searchDto);
}

function callAnalysis(searchDto = {}) {
    $.ajax({
        type: "POST",
        url: getApiResourcePath() + 'sec/analytics/single',
        data: JSON.stringify(searchDto),
        success: function (response) {
            if (response) {
                let normal_number_comma_separator_number_step = $.animateNumber.numberStepFactories.separator(',');
                $('#hdr-total-approved-report').animateNumber({
                    number: response[0].value,
                    numberStep: normal_number_comma_separator_number_step
                });
                $('#hdr-total-approved-str-report').animateNumber({
                    number: response[1].value,
                    numberStep: normal_number_comma_separator_number_step
                });
                $('#hdr-total-approved-ttr-report').animateNumber({
                    number: response[2].value,
                    numberStep: normal_number_comma_separator_number_step
                });
                $('#hdr-total-draft-report').animateNumber({
                    number: response[3].value,
                    numberStep: normal_number_comma_separator_number_step
                });
                $('#hdr-total-waitingapproval-report').animateNumber({
                    number: response[4].value,
                    numberStep: normal_number_comma_separator_number_step
                });
            }
        }
    });
}