# Demo walkthrough

Use a fresh data folder for the demo so the order numbers below start at 1.
On a Mac, run `./run.sh --data-dir demo-1`. On Windows, run
`.\run.bat --data-dir demo-1`. Pick another folder name if that one already
has orders in it.

## Place an order

1. Sign in as `customer` with `customer123`. The demo account picker can fill
   those in for you.
2. Add one hamburger, one fries, and one drink. The total should be $15.47.
3. Click **Place order**. The order should be accepted and assigned to `driver1`.
4. Open **Activity**. Show the `placeOrder` request, the placed and accepted
   appends to `orders.txt`, and the save to `drivers.txt`.

## Deliver it

1. Log out and sign in as `driver1` with `driver123`.
2. Select order 1 and click **Start delivery**. Its status changes to in progress.
3. Select it again and click **Mark delivered**.
4. Show the driver Activity page and the status changes that were saved.

## Rate it

1. Log back in as `customer`.
2. Open **My orders**, select order 1, and click **Rate delivery**.
3. Give it a rating of 5 and save it.
4. The order should now show 5/5. It cannot be rated a second time.

## Check the admin view

1. Sign in as `admin` with `admin123`.
2. Show **All orders**, **Drivers**, and **Activity**. Driver 1 should be
   available with an average of 5.00.
3. Under **Menu**, change the hamburger price to 9.49.
4. Go back to the old order. Its total should still be $15.47.
5. Open Activity to show the menu update request and the save to `Menu.txt`.
6. The Accounts page can create another driver, customer, or admin.

## Show persistence

Close the app and reopen it with the same `--data-dir demo-1` option. The order,
its rating, the driver's average, the updated menu, and the activity log should
still be there. The user files contain password hashes, not the passwords typed
at login.

## Show FIFO assignment

Place three more orders without completing any deliveries. The first two go
to the two available drivers; the third stays placed while it waits. Finish the
first driver's delivery. That driver should automatically get the waiting
order. If several orders are waiting, the oldest one goes first.

## Show the data structures in the code

- `DeliverySystem.ordersQueue` is the FIFO queue.
- `DeliverySystem.readyDrivers` and `Driver.compareTo()` choose the next driver.
- `DeliverySystem.rateDriver()` drops the oldest rating when there are already
  ten. `Driver.restoreRatings()` checks the stored list, and `calculateAvgRating()`
  updates its average. The system also removes and reinserts an available driver
  when that driver's priority changes.
- `TerminalApp.run()` calls `User.viewOptions()` through the shared user type.
  Each subclass supplies a different dashboard menu.
- `FileManager.appendOrder()` appends the order history; `loadOrders()` restores it.

Run `./test.sh --desktop` or `.\test.bat --desktop` to check the complete flows,
including eleven ratings replacing the oldest one. Those checks use temporary
data and run the UI without opening a window.
