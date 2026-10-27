"""배포 명령이 파일 크기와 무관하며 순서·실패 처리를 유지하는지 검증한다."""

import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("deployment", Path(__file__).with_name("prepare_ssm_deployment.py"))
deployment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deployment)


class DeploymentParametersTests(unittest.TestCase):
    def setUp(self):
        self.environment = {
            "AWS_REGION": "ap-northeast-2",
            "ECR_REGISTRY": "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com",
            "ECR_REPOSITORY": "smap-api",
            "IMAGE_TAG": "a" * 40,
        }

    def test_payload_does_not_read_or_embed_sql(self):
        with patch.object(Path, "read_bytes", side_effect=AssertionError("파일을 SSM에 넣으면 안 됩니다.")):
            parameters = deployment.build_parameters(self.environment)
        encoded = json.dumps(parameters).encode("utf-8")
        self.assertLess(len(encoded), 8 * 1024)
        self.assertNotIn("base64", encoded.decode())
        self.assertNotIn("INSERT INTO", encoded.decode())
        self.assertEqual(["600"], parameters["executionTimeout"])

    def test_image_files_are_ready_before_database_and_app_start(self):
        commands = deployment.build_parameters(self.environment)["commands"]
        pull = next(i for i, value in enumerate(commands) if value.startswith("docker pull"))
        copy = next(i for i, value in enumerate(commands) if value.startswith("docker cp"))
        sql = next(i for i, value in enumerate(commands) if value.startswith("for f in db/init"))
        start = next(i for i, value in enumerate(commands) if value.endswith("--wait-timeout 180"))
        self.assertLess(pull, copy)
        self.assertLess(copy, sql)
        self.assertLess(sql, start)
        self.assertIn("set -eu", commands)
        self.assertTrue(any("ON_ERROR_STOP=1" in value for value in commands))
        self.assertTrue(any("previous-init" in value for value in commands))
        self.assertTrue(any(value.startswith("trap ") for value in commands))
        self.assertFalse(any("-delete" in value for value in commands))

    def test_missing_or_oversized_configuration_is_rejected(self):
        self.environment.pop("IMAGE_TAG")
        with self.assertRaises(ValueError):
            deployment.build_parameters(self.environment)
        self.environment["IMAGE_TAG"] = "a" * 100000
        with self.assertRaises(ValueError):
            deployment.build_parameters(self.environment)


if __name__ == "__main__":
    unittest.main()
