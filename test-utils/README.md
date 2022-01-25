<!--

     Copyright (c) 2020 - 2022 Henix, henix.fr

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
# OpenTestFactory Java toolkit -> test-utils

Test utils module used by plugins. The goal of this module is to expose convenient methods for writing unit tests (Pure in memory tests). Integration test code should be in test-harness-tools. 

This module is designed to be used by toolkit consumers, typically with a maven dep scoped as test... **It is not dedicated to factorize the test code of the toolkit itself**, hence the last position in maven build order.
