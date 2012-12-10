#
# Sonar, open source software quality management tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

#
# Sonar 3.4
#
class IgnoreHomeDashboardOnUpgrades < ActiveRecord::Migration

  class LoadedTemplate < ActiveRecord::Base
  end

  def self.up
    if LoadedTemplate.count(:all)>0 && !LoadedTemplate.exists?({:template_type => 'DASHBOARD', :kee => 'Home'})
      LoadedTemplate.create({:template_type => 'DASHBOARD', :kee => 'Home'})
    end
  end

end
