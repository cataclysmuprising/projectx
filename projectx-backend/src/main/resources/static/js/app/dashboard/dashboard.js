function init() {
    initializeTargetMonthPicker();
    loadDashboard();
}

function bind() {
    $('#btnRefreshDashboard')
        .off('click.dashboardRefresh')
        .on('click.dashboardRefresh', function () {
            loadDashboard();
        });

    $('#targetMonthPicker')
        .off('dp.change.dashboardMonth')
        .on('dp.change.dashboardMonth', function (event) {
            if (!event || !event.date) {
                return;
            }
            loadDashboard();
        });
}

function initializeTargetMonthPicker() {
    if (typeof moment === 'undefined') {
        return;
    }

    const picker = $('#targetMonthPicker');
    if (picker.length === 0 || typeof picker.datetimepicker !== 'function') {
        return;
    }

    if (picker.data('DateTimePicker')) {
        picker.datetimepicker('destroy');
    }

    picker.datetimepicker({
        viewMode: 'months',
        format: 'MMMM / YYYY',
        maxDate: moment(),
        dayViewHeaderFormat: 'MMMM YYYY',
        useCurrent: true,
        ignoreReadonly: true,
        allowInputToggle: true,
        focusOnShow: false,
        widgetPositioning: {
            horizontal: 'right',
            vertical: 'bottom'
        }
    });

    const widget = picker.data('DateTimePicker');
    if (widget) {
        widget.date(moment().startOf('month'));
    }

    picker.find('.datepickerbutton, input')
        .off('click.dashboardPicker')
        .on('click.dashboardPicker', function (event) {
            event.preventDefault();
            const current = picker.data('DateTimePicker');
            if (current) {
                current.show();
            }
        });
}

function loadDashboard() {
    // render dashboard contents
}

function resolveTargetMonth() {
    const picker = $('#targetMonthPicker').data('DateTimePicker');
    const selectedDate = picker && typeof picker.date === 'function'
        ? picker.date()
        : null;
    if (!selectedDate || !selectedDate.isValid()) {
        return null;
    }

    return {
        year: selectedDate.year(),
        month: selectedDate.month() + 1
    };
}

