"""Offline IPC regression tests; no game process or account interaction."""
import sys
import unittest
import tempfile
from unittest.mock import patch
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
import hso_tool
from hso_tool import HsoStudioApp


class RemoteSellToolTests(unittest.TestCase):
    def app(self):
        app = HsoStudioApp.__new__(HsoStudioApp)
        app.state_data = {"in_game": True, "map_id": 7, "area": 3,
                          "map_monsters": [{"id": 2, "name": "Sói", "hp": 100}]}
        app.config = {"auto_farm": {"7:*": {"all": True}}, "farm_sync": True,
                      "auto_sell_filter": {"white": True, "unlocked_only": True}}
        app.pending_sell_until = 0.0
        app.last_farm_signature = None
        app.commands = []
        app.messages = []
        app.write_command = app.commands.append
        app.log_msg = app.messages.append
        return app

    def test_unchanged_farm_profile_does_not_flood_sale_queue(self):
        app = self.app()
        app.sync_farm_targets()
        app.sync_farm_targets()
        self.assertEqual(len(app.commands), 1)
        self.assertEqual(app.commands[0]["ids"], "2")
        app.state_data["area"] = 4
        app.sync_farm_targets()
        self.assertEqual(len(app.commands), 2)

    def test_failed_ipc_write_can_be_retried(self):
        app = self.app()
        def fail(command):
            raise OSError("simulated failure")
        app.write_command = fail
        app.sync_farm_targets()
        self.assertIsNone(app.last_farm_signature)
        app.write_command = app.commands.append
        app.sync_farm_targets()
        self.assertEqual(len(app.commands), 1)

    def test_single_sale_carries_explicit_lock_confirmation(self):
        app = self.app()
        app.send_sell_single_item(27, 3, "Vòng bình an", allow_locked=True)
        self.assertEqual(app.commands[0], {"action": "sell_single_item", "id": 27,
                                          "cat": 3, "allow_locked": True, "item_name": "Vòng bình an"})
        app.send_sell_single_item(28, 3, "Gậy gỗ")
        self.assertEqual(len(app.commands), 1, "rapid duplicate click must be blocked")

    def test_do_not_queue_sales_when_server_reports_busy(self):
        app = self.app()
        app.state_data["remote_sell"] = {"active": True}
        app.send_sell_now_command()
        app.send_sell_single_item(27)
        self.assertEqual(app.commands, [])

    def test_do_not_send_sales_before_login(self):
        app = self.app()
        app.state_data["in_game"] = False
        app.send_sell_now_command()
        self.assertEqual(app.commands, [])

    def test_batch_command_uses_current_filter(self):
        app = self.app()
        app.send_sell_now_command()
        self.assertTrue(app.commands[0]["unlocked_only"])
        self.assertEqual(app.commands[0]["action"], "sell_now")

    def test_graham_button_opens_menu_without_movement_or_sale(self):
        app = self.app()
        app.open_graham_npc()
        self.assertEqual(app.commands, [{"action": "test_npc_packet", "npc_id": -58, "x": 0, "y": 0}])
        app.open_graham_npc()
        self.assertEqual(len(app.commands), 1)

    def test_graham_button_does_not_interrupt_sale_or_queue_before_login(self):
        app = self.app()
        app.state_data["remote_sell"] = {"active": True}
        app.open_graham_npc()
        app.state_data["remote_sell"]["active"] = False
        app.state_data["in_game"] = False
        app.open_graham_npc()
        self.assertEqual(app.commands, [])

    def test_install_staged_jar_on_next_launch(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "game.jar"
            pending = Path(folder) / "game.jar.pending"
            target.write_bytes(b"old")
            pending.write_bytes(b"verified-new")
            with patch.object(hso_tool, "JAR_TARGET", target), patch.object(hso_tool, "JAR_PENDING", pending):
                self.assertTrue(hso_tool.apply_pending_jar())
                self.assertEqual(target.read_bytes(), b"verified-new")
                self.assertFalse(pending.exists())
                self.assertFalse(hso_tool.apply_pending_jar())

    def test_locked_jar_keeps_pending_update(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "game.jar"
            pending = Path(folder) / "game.jar.pending"
            target.write_bytes(b"old")
            pending.write_bytes(b"verified-new")
            with patch.object(hso_tool, "JAR_TARGET", target), patch.object(hso_tool, "JAR_PENDING", pending), \
                    patch.object(hso_tool.os, "replace", side_effect=PermissionError("locked")):
                with self.assertRaises(PermissionError):
                    hso_tool.apply_pending_jar()
            self.assertEqual(target.read_bytes(), b"old")
            self.assertEqual(pending.read_bytes(), b"verified-new")

    def test_staged_update_does_not_overwrite_a_newer_build(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "game.jar"
            pending = Path(folder) / "game.jar.pending"
            target.write_bytes(b"newer-manual-build")
            pending.write_bytes(b"older-staged-build")
            hso_tool.os.utime(pending, ns=(1_000_000_000, 1_000_000_000))
            hso_tool.os.utime(target, ns=(2_000_000_000, 2_000_000_000))
            with patch.object(hso_tool, "JAR_TARGET", target), patch.object(hso_tool, "JAR_PENDING", pending):
                self.assertFalse(hso_tool.apply_pending_jar())
            self.assertEqual(target.read_bytes(), b"newer-manual-build")


if __name__ == "__main__":
    unittest.main()
