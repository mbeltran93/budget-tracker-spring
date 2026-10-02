// @ts-check
const { test, expect } = require('@playwright/test');

const DEMO_EMAIL = 'demo@budgettracker.com';
const DEMO_PASSWORD = 'demo1234';

/**
 * Simula a un usuario real navegando toda la app: login, ver el dashboard con
 * los datos sembrados por Flyway, dar de alta/editar/borrar un gasto por el modal
 * AJAX (sin recargar la pagina) comprobando que la tabla, el total, el grafico y
 * la barra de progreso se actualizan solos, crear una categoria con el flujo
 * clasico de formulario + recarga, y por ultimo cerrar sesion.
 */
test.describe('Flujo completo de Budget Tracker', () => {

    test('login, dashboard, CRUD de gastos por AJAX, alta de categoria y logout', async ({ page }) => {
        // 1. Login
        await page.goto('/login');
        await expect(page.locator('h3')).toContainText('Budget Tracker');

        await page.fill('#username', DEMO_EMAIL);
        await page.fill('#password', DEMO_PASSWORD);
        await page.click('button[type="submit"]');

        // 2. Dashboard
        await expect(page).toHaveURL(/\/dashboard$/);
        await expect(page.locator('h2#totalThisMonth')).toBeVisible();
        const totalBefore = await page.locator('#totalThisMonth').innerText();

        await expect(page.locator('#budgetProgressList')).toContainText('Comida');
        await expect(page.locator('canvas#categoryChart')).toBeVisible();

        const comidaCard = page.locator('#budgetProgressList > div', { hasText: 'Comida' });
        const comidaAmountsBefore = await comidaCard.locator('.budget-amounts').innerText();

        // 3. Agregar un gasto via el modal AJAX
        const rowCountBefore = await page.locator('#expensesTable tbody tr').count();

        await page.click('#btnAddExpense');
        await expect(page.locator('#expenseModal')).toBeVisible();
        await page.selectOption('#expenseCategory', { label: 'Comida' });
        await page.fill('#expenseAmount', '77.50');
        await page.fill('#expenseDescription', 'Playwright - compra de prueba');
        // la fecha ya viene precargada con el dia de hoy
        await page.click('#btnSaveExpense');

        // El modal se cierra solo y la fila aparece sin recargar la pagina
        await expect(page.locator('#expenseModal')).toBeHidden();
        await expect(page.locator('#expensesTable tbody tr')).toHaveCount(rowCountBefore + 1);
        const newRow = page.locator('#expensesTable tbody tr').first();
        await expect(newRow).toContainText('Playwright - compra de prueba');
        await expect(newRow).toContainText('$77.50');

        // El total y la barra de progreso de Comida se actualizaron sin recargar
        await expect(page.locator('#totalThisMonth')).not.toHaveText(totalBefore);
        await expect(comidaCard.locator('.budget-amounts')).not.toHaveText(comidaAmountsBefore);

        // 4. Editar ese gasto
        await newRow.locator('.btn-edit-expense').click();
        await expect(page.locator('#expenseModalTitle')).toHaveText('Editar gasto');
        await expect(page.locator('#expenseAmount')).toHaveValue('77.5');
        await page.fill('#expenseAmount', '120.00');
        await page.fill('#expenseDescription', 'Playwright - compra editada');
        await page.click('#btnSaveExpense');

        await expect(page.locator('#expenseModal')).toBeHidden();
        const editedRow = page.locator('#expensesTable tbody tr').filter({ hasText: 'Playwright - compra editada' });
        await expect(editedRow).toContainText('$120.00');

        // 5. Borrar el gasto
        page.once('dialog', dialog => dialog.accept());
        await editedRow.locator('.btn-delete-expense').click();
        await expect(page.locator('#expensesTable tbody tr').filter({ hasText: 'Playwright - compra editada' }))
            .toHaveCount(0);
        await expect(page.locator('#expensesTable tbody tr')).toHaveCount(rowCountBefore);

        // 6. Crear una categoria nueva: flujo clasico con recarga de pagina (th:action)
        const categoryName = `Playwright E2E ${Date.now()}`;
        await page.click('text=Categorias');
        await expect(page).toHaveURL(/\/categories$/);
        await page.click('text=+ Nueva categoria');
        await expect(page).toHaveURL(/\/categories\/new$/);

        await page.fill('#name', categoryName);
        await page.fill('#monthlyLimit', '150.00');
        await page.click('#btnSaveCategory');

        await expect(page).toHaveURL(/\/categories$/);
        await expect(page.locator('table')).toContainText(categoryName);

        // Limpieza: borra la categoria creada para que el test sea repetible
        const categoryRow = page.locator('tr', { hasText: categoryName });
        page.once('dialog', dialog => dialog.accept());
        await categoryRow.locator('button', { hasText: 'Borrar' }).click();
        await expect(page.locator('table')).not.toContainText(categoryName);

        // 7. Logout
        await page.click('button:has-text("Salir")');
        await expect(page).toHaveURL(/\/login/);
        await expect(page.locator('.alert-success')).toContainText('Sesion cerrada');
    });

    test('un usuario anonimo es redirigido a /login al intentar ver el dashboard', async ({ page }) => {
        await page.goto('/dashboard');
        await expect(page).toHaveURL(/\/login/);
    });
});
