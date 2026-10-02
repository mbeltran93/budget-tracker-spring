/**
 * Toda la interactividad del dashboard: alta/edicion/borrado de gastos via $.ajax
 * (sin recargar la pagina) y el refresco del total, el grafico de Chart.js y las
 * barras de progreso de presupuesto por categoria.
 */
$(function () {
    'use strict';

    var STATUS_TO_BOOTSTRAP_CLASS = {OK: 'success', WARNING: 'warning', OVER: 'danger'};
    var CHART_COLORS = ['#0d6efd', '#fd7e14', '#20c997', '#6f42c1', '#dc3545', '#198754', '#ffc107'];

    var $modal = $('#expenseModal');
    var $form = $('#expenseForm');
    var categoryChart = null;

    initChart(readInitialBudgetOverview());
    bindEvents();

    function readInitialBudgetOverview() {
        var raw = $('#initialBudgetOverview').text();
        try {
            return JSON.parse(raw || '[]');
        } catch (e) {
            return [];
        }
    }

    function bindEvents() {
        $('#btnAddExpense').on('click', function () {
            openModalForCreate();
        });

        $(document).on('click', '.btn-edit-expense', function () {
            var $row = $(this).closest('tr');
            openModalForEdit($row);
        });

        $(document).on('click', '.btn-delete-expense', function () {
            var $row = $(this).closest('tr');
            if (window.confirm('Borrar este gasto?')) {
                deleteExpense($row);
            }
        });

        $form.on('submit', function (event) {
            event.preventDefault();
            if (!validateForm()) {
                return;
            }
            saveExpense();
        });

        $modal.on('hidden.bs.modal', function () {
            resetForm();
        });
    }

    function openModalForCreate() {
        resetForm();
        $('#expenseModalTitle').text('Nuevo gasto');
        $('#expenseDate').val(todayIsoDate());
        $modal.modal('show');
    }

    function openModalForEdit($row) {
        resetForm();
        $('#expenseModalTitle').text('Editar gasto');
        $('#expenseId').val($row.data('id'));
        $('#expenseCategory').val($row.data('category-id'));
        $('#expenseAmount').val($row.data('amount'));
        $('#expenseDescription').val($row.data('description'));
        $('#expenseDate').val($row.data('date'));
        $modal.modal('show');
    }

    function resetForm() {
        $form[0].reset();
        $('#expenseId').val('');
        $form.find('.is-invalid').removeClass('is-invalid');
        $form.find('.invalid-feedback').text('');
        $('#expenseFormError').addClass('d-none').text('');
    }

    function todayIsoDate() {
        var now = new Date();
        var month = String(now.getMonth() + 1).padStart(2, '0');
        var day = String(now.getDate()).padStart(2, '0');
        return now.getFullYear() + '-' + month + '-' + day;
    }

    /** Validacion basica en el cliente, antes de pegarle al servidor (que vuelve a validar con @Valid). */
    function validateForm() {
        var valid = true;
        $form.find('.is-invalid').removeClass('is-invalid');

        var categoryId = $('#expenseCategory').val();
        if (!categoryId) {
            markInvalid('expenseCategory', 'errorCategoryId', 'Elegi una categoria');
            valid = false;
        }

        var amount = parseFloat($('#expenseAmount').val());
        if (isNaN(amount) || amount <= 0) {
            markInvalid('expenseAmount', 'errorAmount', 'El monto debe ser mayor a 0');
            valid = false;
        }

        var description = $('#expenseDescription').val() || '';
        if (description.length > 255) {
            markInvalid('expenseDescription', 'errorDescription', 'La descripcion es demasiado larga');
            valid = false;
        }

        var date = $('#expenseDate').val();
        if (!date) {
            markInvalid('expenseDate', 'errorDate', 'La fecha es obligatoria');
            valid = false;
        } else if (date > todayIsoDate()) {
            markInvalid('expenseDate', 'errorDate', 'La fecha no puede ser futura');
            valid = false;
        }

        return valid;
    }

    function markInvalid(fieldId, errorId, message) {
        $('#' + fieldId).addClass('is-invalid');
        $('#' + errorId).text(message);
    }

    function saveExpense() {
        var id = $('#expenseId').val();
        var payload = {
            categoryId: parseInt($('#expenseCategory').val(), 10),
            amount: parseFloat($('#expenseAmount').val()),
            description: $('#expenseDescription').val(),
            date: $('#expenseDate').val()
        };

        var request = id
            ? $.ajax({url: '/api/expenses/' + id, method: 'PUT', contentType: 'application/json', data: JSON.stringify(payload)})
            : $.ajax({url: '/api/expenses', method: 'POST', contentType: 'application/json', data: JSON.stringify(payload)});

        request.done(function (expense) {
            $modal.modal('hide');
            if (id) {
                replaceRow(expense);
            } else {
                prependRow(expense);
            }
            refreshSummary();
        }).fail(function (xhr) {
            showServerError(xhr);
        });
    }

    function showServerError(xhr) {
        var message = 'No se pudo guardar el gasto.';
        if (xhr.responseJSON && xhr.responseJSON.message) {
            message = xhr.responseJSON.message;
        }
        $('#expenseFormError').removeClass('d-none').text(message);
    }

    function deleteExpense($row) {
        var id = $row.data('id');
        $.ajax({url: '/api/expenses/' + id, method: 'DELETE'}).done(function () {
            $row.remove();
            maybeShowEmptyMessage();
            refreshSummary();
        }).fail(function () {
            window.alert('No se pudo borrar el gasto.');
        });
    }

    function maybeShowEmptyMessage() {
        if ($('#expensesTable tbody tr').length === 0) {
            $('#noExpensesMessage').removeClass('d-none');
        }
    }

    function rowHtml(expense) {
        var amountFormatted = '$' + Number(expense.amount).toFixed(2);
        return '<tr id="expense-row-' + expense.id + '" class="expense-row-new"' +
            ' data-id="' + expense.id + '"' +
            ' data-category-id="' + expense.categoryId + '"' +
            ' data-amount="' + expense.amount + '"' +
            ' data-description="' + escapeHtml(expense.description || '') + '"' +
            ' data-date="' + expense.date + '">' +
            '<td>' + expense.date + '</td>' +
            '<td>' + escapeHtml(expense.categoryName) + '</td>' +
            '<td>' + escapeHtml(expense.description || '') + '</td>' +
            '<td class="text-end expense-amount">' + amountFormatted + '</td>' +
            '<td class="text-end">' +
            '<button type="button" class="btn btn-sm btn-outline-secondary btn-edit-expense">Editar</button> ' +
            '<button type="button" class="btn btn-sm btn-outline-danger btn-delete-expense">Borrar</button>' +
            '</td>' +
            '</tr>';
    }

    function prependRow(expense) {
        $('#noExpensesMessage').addClass('d-none');
        $('#expensesTable tbody').prepend(rowHtml(expense));
    }

    function replaceRow(expense) {
        $('#expense-row-' + expense.id).replaceWith(rowHtml(expense));
    }

    function escapeHtml(value) {
        return $('<div>').text(value).html();
    }

    function refreshSummary() {
        $.ajax({url: '/api/dashboard/summary', method: 'GET'}).done(function (summary) {
            $('#totalThisMonth').text('$' + Number(summary.totalThisMonth).toFixed(2));
            updateProgressBars(summary.categories);
            updateChart(summary.categories);
        });
    }

    function updateProgressBars(categories) {
        categories.forEach(function (item) {
            var $container = $('#budgetProgressList [data-category-id="' + item.categoryId + '"]');
            if ($container.length === 0) {
                return;
            }
            $container.find('.budget-amounts').html(
                '$' + Number(item.spent).toFixed(2) + ' / $' + Number(item.monthlyLimit).toFixed(2)
            );
            var $bar = $container.find('.budget-progress-bar');
            $bar.removeClass('bg-success bg-warning bg-danger')
                .addClass('bg-' + (STATUS_TO_BOOTSTRAP_CLASS[item.status] || 'success'))
                .css('width', item.percentUsed + '%')
                .text(item.percentUsed + '%');
        });
    }

    function initChart(categories) {
        var ctx = document.getElementById('categoryChart');
        if (!ctx || typeof Chart === 'undefined') {
            return;
        }
        categoryChart = new Chart(ctx, {
            type: 'doughnut',
            data: buildChartData(categories),
            options: {responsive: true, maintainAspectRatio: false}
        });
    }

    function updateChart(categories) {
        if (!categoryChart) {
            initChart(categories);
            return;
        }
        var data = buildChartData(categories);
        categoryChart.data = data;
        categoryChart.update();
    }

    function buildChartData(categories) {
        return {
            labels: categories.map(function (item) {
                return item.categoryName;
            }),
            datasets: [{
                data: categories.map(function (item) {
                    return Number(item.spent);
                }),
                backgroundColor: categories.map(function (item, index) {
                    return CHART_COLORS[index % CHART_COLORS.length];
                })
            }]
        };
    }
});
